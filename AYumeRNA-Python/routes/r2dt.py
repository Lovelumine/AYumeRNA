import os
import subprocess
import shutil
from pathlib import Path
from flask import Blueprint, request, jsonify
from datetime import datetime

r2dt_bp = Blueprint('r2dt', __name__, url_prefix='/r2dt')

# 设置 R2DT 相关路径
R2DT_LIBRARY = "/home/yingying/2.0"  # 替换为实际的预计算数据路径
DOCKER_IMAGE = "rnacentral/r2dt"
TEMP_DIR_BASE = Path("/home/yingying/Documents/AYumeRNA/AYumeRNA-Python/temp_files")  # 临时文件存储在当前目录下

# 创建一个简单的计数器
request_counter = 0

@r2dt_bp.route('/run', methods=['POST'])
def run_r2dt():
    """接受 RNA 序列并返回点括号结构和 SVG 文件"""
    global request_counter
    temp_dir = None  # 预定义临时目录变量
    try:
        print("[INFO] Received request to run R2DT.")
        
        # 从请求中获取 RNA 序列
        data = request.json
        if not data or "sequence" not in data:
            print("[ERROR] Invalid input: 'sequence' is missing.")
            return jsonify({"error": "Invalid input, 'sequence' is required"}), 400
        
        rna_sequence = data["sequence"].strip()
        print(f"[INFO] RNA sequence received: {rna_sequence[:50]}...")  # 显示前50个字符
        if not rna_sequence.startswith(">"):
            rna_sequence = f">input_sequence\n{rna_sequence}"

        # 创建基于时间戳和计数器的临时工作目录
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        temp_dir = TEMP_DIR_BASE / f"r2dt_{timestamp}_{request_counter}"
        request_counter += 1
        temp_dir.mkdir(parents=True, exist_ok=True)
        temp_path = Path(temp_dir)
        fasta_file = temp_path / "input_sequence.fasta"
        output_dir = temp_path / "r2dt_output"
        output_dir.mkdir(exist_ok=True)

        print(f"[INFO] Temporary directory created: {temp_path}")
        print(f"[INFO] Writing RNA sequence to file: {fasta_file}")

        # 写入 FASTA 文件
        with open(fasta_file, "w") as f:
            f.write(rna_sequence)
        
        print(f"[INFO] FASTA file written successfully: {fasta_file}")

        # 构建 Docker 命令
        docker_command = [
            "docker", "run", "--rm", 
            "-v", f"{R2DT_LIBRARY}:/rna/r2dt/data/cms",  # 挂载 R2DT 数据库
            "-v", f"{temp_dir}:/rna/r2dt/temp",  # 挂载临时目录
            DOCKER_IMAGE,
            "r2dt.py", "draw",
            "/rna/r2dt/temp/input_sequence.fasta",
            "/rna/r2dt/temp/r2dt_output"
        ]

        print(f"[INFO] Running Docker command: {' '.join(docker_command)}")

        # 执行 Docker 命令
        subprocess.run(docker_command, check=True)
        print("[INFO] Docker command executed successfully.")

        # 检查生成的结果文件
        dot_bracket_file = output_dir / "results/fasta/input_sequence-E_Phe.fasta"
        svg_colored = output_dir / "results/svg/input_sequence-E_Phe.colored.svg"
        svg_enriched = output_dir / "results/svg/input_sequence-E_Phe.enriched.svg"
        thumbnail_svg = output_dir / "results/thumbnail/input_sequence-E_Phe.thumbnail.svg"

        print(f"[INFO] Checking output files in: {output_dir}")
        missing_files = []
        if not dot_bracket_file.exists():
            missing_files.append(str(dot_bracket_file))
        if not svg_colored.exists():
            missing_files.append(str(svg_colored))
        if not svg_enriched.exists():
            missing_files.append(str(svg_enriched))
        if not thumbnail_svg.exists():
            missing_files.append(str(thumbnail_svg))
        
        if missing_files:
            print(f"[ERROR] Missing output files: {', '.join(missing_files)}")
            return jsonify({"error": f"R2DT run failed. Missing files: {', '.join(missing_files)}"}), 500

        # 读取点括号二级结构
        dot_bracket = None
        with open(dot_bracket_file, "r") as f:
            lines = f.readlines()
            dot_bracket = lines[1].strip() if len(lines) > 1 else None
        
        print(f"[INFO] Dot-bracket structure extracted: {dot_bracket}")

        # 读取 SVG 文件内容
        svg_files = {
            "colored_svg": svg_colored.read_text(),
            "enriched_svg": svg_enriched.read_text(),
            "thumbnail_svg": thumbnail_svg.read_text()
        }
        print("[INFO] SVG files loaded successfully.")

        # 返回结果
        print("[INFO] Returning R2DT results.")
        return jsonify({
            "dot_bracket": dot_bracket,
            "svg_files": svg_files
        })

    except subprocess.CalledProcessError as e:
        print(f"[ERROR] R2DT execution failed: {e}")
        return jsonify({"error": f"R2DT execution failed: {str(e)}"}), 500
    except Exception as e:
        print(f"[ERROR] Unexpected error occurred: {e}")
        return jsonify({"error": f"Unexpected error: {str(e)}"}), 500
    finally:
        # 强制清理临时文件夹
        if temp_dir:
            try:
                shutil.rmtree(temp_dir, ignore_errors=True)
                print(f"[INFO] Temporary files deleted: {temp_dir}")
            except Exception as cleanup_error:
                print(f"[ERROR] Failed to delete temporary files: {cleanup_error}")
