import os
import subprocess
import tempfile
from flask import Blueprint, request, jsonify, send_from_directory

# 创建蓝图
scape_bp = Blueprint('scape', __name__)

def run_command(command):
    """执行命令行命令"""
    result = subprocess.run(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode != 0:
        raise RuntimeError(f"Command failed: {command}\nError: {result.stderr.decode()}")
    return result.stdout.decode()

@scape_bp.route('/rscape', methods=['POST'])
def rscape_analysis():
    """
    用户上传文件，运行 R-scape 分析，并返回结果
    """
    try:
        # 1. 确保用户上传了文件
        if 'file' not in request.files:
            return jsonify({"error": "No file provided"}), 400
        
        uploaded_file = request.files['file']
        
        # 2. 在临时目录中保存上传的文件
        with tempfile.TemporaryDirectory() as temp_dir:
            input_fasta = os.path.join(temp_dir, uploaded_file.filename)
            uploaded_file.save(input_fasta)

            # 3. 定义临时文件名
            aligned_fasta = os.path.join(temp_dir, "aligned_sequences.fasta")
            aligned_sto = os.path.join(temp_dir, "aligned_sequences.sto")
            rnaalifold_input = os.path.join(temp_dir, "alifold_input.msa")
            rnaalifold_output = os.path.join(temp_dir, "alifold_results.txt")
            final_sto = os.path.join(temp_dir, "final_with_SS_cons.sto")
            rscape_output_dir = os.path.join(temp_dir, "rscape_results")

            # Step 1: MAFFT生成对齐文件
            run_command(f"mafft --auto {input_fasta} > {aligned_fasta}")

            # Step 2: 转换为Stockholm格式
            run_command(f"esl-reformat stockholm {aligned_fasta} > {aligned_sto}")

            # Step 3: 使用 RNAalifold 生成共识结构
            run_command(f"esl-reformat clustal {aligned_sto} > {rnaalifold_input}")
            run_command(f"RNAalifold --aln {rnaalifold_input} > {rnaalifold_output}")

            # 提取共识结构
            consensus_structure = None
            with open(rnaalifold_output, "r") as f:
                for line in f:
                    if "(" in line and ")" in line:  # 结构行
                        consensus_structure = line.strip().split()[0]
                        break
            if not consensus_structure:
                return jsonify({"error": "Consensus structure not found"}), 500

            # Step 4: 添加共识结构到STO文件
            with open(aligned_sto, "r") as infile:
                lines = infile.readlines()
            if lines[-1].strip() == "//":
                lines.pop()
            lines = [line for line in lines if not line.startswith("#=GC SS_cons")]
            lines.append(f"#=GC SS_cons {consensus_structure}\n")
            lines.append("//\n")
            with open(final_sto, "w") as outfile:
                outfile.writelines(lines)

            # Step 5: 运行 R-scape
            os.makedirs(rscape_output_dir, exist_ok=True)
            run_command(f"R-scape -s -E 0.05 --outdir {rscape_output_dir} {final_sto}")

            # 返回 R-scape 输出目录下的所有结果文件
            result_files = os.listdir(rscape_output_dir)
            results = {
                "rscape_results": result_files,
                "rscape_dir": rscape_output_dir
            }

            # 打包结果文件发送
            return send_from_directory(rscape_output_dir, result_files[0])

    except Exception as e:
        return jsonify({"error": str(e)}), 500