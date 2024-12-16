import os
import subprocess
import tempfile
import zipfile
from flask import Blueprint, request, jsonify, send_file

# 创建蓝图
scape_bp = Blueprint('scape', __name__)

def run_command(command):
    """执行命令行命令并输出结果"""
    print(f"Running command: {command}")  # 输出正在执行的命令
    result = subprocess.run(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode != 0:
        error_message = result.stderr.decode()
        print(f"Command failed with error: {error_message}")  # 输出错误信息
        raise RuntimeError(f"Command failed: {command}\nError: {error_message}")
    print(f"Command output: {result.stdout.decode()}")  # 输出命令的标准输出
    return result.stdout.decode()

@scape_bp.route('/scape/analyze', methods=['POST'])
def rscape_analysis():
    """
    用户上传文件，运行 R-scape 分析，并返回结果
    """
    try:
        # 1. 确保用户上传了文件
        if 'file' not in request.files:
            return jsonify({"error": "No file provided"}), 400
        uploaded_file = request.files['file']
        print(f"Received file: {uploaded_file.filename}")

        # 2. 在临时目录中保存上传的文件
        with tempfile.TemporaryDirectory() as temp_dir:
            input_fasta = os.path.join(temp_dir, uploaded_file.filename)
            uploaded_file.save(input_fasta)
            print(f"File saved to {input_fasta}")

            # 读取并打印输入文件内容
            with open(input_fasta, 'r') as f:
                file_content = f.read()
            print(f"Input FASTA content:\n{file_content[:500]}")  # 打印文件内容的前500个字符

            # 3. 定义临时文件名
            aligned_fasta = os.path.join(temp_dir, "aligned_sequences.fasta")
            aligned_sto = os.path.join(temp_dir, "aligned_sequences.sto")
            rnaalifold_input = os.path.join(temp_dir, "alifold_input.msa")
            rnaalifold_output = os.path.join(temp_dir, "alifold_results.txt")
            final_sto = os.path.join(temp_dir, "final_with_SS_cons.sto")
            rscape_output_dir = os.path.join(temp_dir, "rscape_results")

            # Step 1: MAFFT生成对齐文件
            print("Running MAFFT to generate alignment...")
            run_command(f"mafft --auto {input_fasta} > {aligned_fasta}")
            print(f"Alignment saved to {aligned_fasta}")

            # 读取并打印 MAFFT 对齐文件内容
            with open(aligned_fasta, 'r') as f:
                aligned_content = f.read()
            print(f"Aligned FASTA content:\n{aligned_content[:500]}")  # 打印对齐文件的前500个字符

            # Step 2: 转换为Stockholm格式
            print(f"Running esl-reformat to convert to Stockholm format...")
            run_command(f"esl-reformat stockholm {aligned_fasta} > {aligned_sto}")
            print(f"Stockholm format saved to {aligned_sto}")

            # Step 3: 使用 RNAalifold 生成共识结构
            print(f"Running esl-reformat to convert Stockholm to Clustal format...") 
            run_command(f"esl-reformat clustal {aligned_sto} > {rnaalifold_input}")
            print(f"Clustal format saved to {rnaalifold_input}")
            print("Running RNAalifold to generate consensus structure...")
            run_command(f"RNAalifold --aln {rnaalifold_input} > {rnaalifold_output}")
            print(f"RNAalifold output saved to {rnaalifold_output}")

            # 提取共识结构
            consensus_structure = None
            with open(rnaalifold_output, "r") as f:
                for line in f:
                    if "(" in line and ")" in line:  # 结构行
                        consensus_structure = line.strip().split()[0]
                        break
            if not consensus_structure:
                print("No consensus structure found in RNAalifold output.")
                return jsonify({"error": "Consensus structure not found"}), 500
            print(f"Consensus structure: {consensus_structure}")

            # Step 4: 添加共识结构到STO文件
            print(f"Adding consensus structure to Stockholm format...")
            with open(aligned_sto, "r") as infile:
                lines = infile.readlines()
            if lines[-1].strip() == "//":
                lines.pop()
            lines = [line for line in lines if not line.startswith("#=GC SS_cons")]
            lines.append(f"#=GC SS_cons {consensus_structure}\n")
            lines.append("//\n")
            with open(final_sto, "w") as outfile:
                outfile.writelines(lines)
            print(f"Final Stockholm file with consensus saved to {final_sto}")

            # Step 5: 运行 R-scape
            print(f"Running R-scape with final Stockholm file...")
            os.makedirs(rscape_output_dir, exist_ok=True)
            run_command(f"R-scape -s -E 0.05 --outdir {rscape_output_dir} {final_sto}")
            print(f"R-scape results saved to {rscape_output_dir}")

            # 返回 R-scape 输出目录下的所有结果文件
            result_files = os.listdir(rscape_output_dir)
            print(f"R-scape result files: {result_files}")

            # 打包多个结果文件为zip
            zip_filename = os.path.join(temp_dir, 'rscape_results.zip')
            with zipfile.ZipFile(zip_filename, 'w', zipfile.ZIP_DEFLATED) as zipf:
                for file in result_files:
                    zipf.write(os.path.join(rscape_output_dir, file), file)
            print(f"Results zipped into: {zip_filename}")

            # 返回zip文件
            return send_file(zip_filename, as_attachment=True)

    except Exception as e:
        print(f"Error occurred: {str(e)}")  # 输出错误信息
        return jsonify({"error": str(e)}), 500
