import os
from flask import Blueprint, request, jsonify
from scripts.make_onehot_from_traceback import make_onehot_of_cm_from_traceback
from helpers import download_file_from_minio, run_cmalign, upload_to_minio
import tempfile

# 创建 Blueprint
traceback_bp = Blueprint('traceback', __name__)

# 路由处理
@traceback_bp.route('/process_traceback', methods=['POST'])
def handle_process_traceback():
    try:
        # 获取请求的 JSON 数据
        data = request.get_json()
        print(f"Received data: {data}")

        # 从 JSON 中获取 MinIO 上的 FASTA 文件和 CM 文件 URL
        fasta_url = data.get('traceback')
        cmfile_url = data.get('cmfile')
        cpu_cores = data.get('cpu', 4)
        user_id = data.get('user_id', 'unknown_user')

        if not fasta_url or not cmfile_url:
            print("Missing traceback or cmfile in the request")
            return jsonify({"error": "Missing traceback or cmfile"}), 400

        # 使用系统临时文件夹路径
        temp_dir = tempfile.gettempdir()
        fasta_file_path = os.path.join(temp_dir, "task_fasta.fasta")
        cmfile_path = os.path.join(temp_dir, "task_cm.cm")

        # 下载 MinIO 上的文件到本地
        download_file_from_minio(fasta_url, fasta_file_path)
        download_file_from_minio(cmfile_url, cmfile_path)

        # 调用处理逻辑
        progress_messages = []
        gz_traceback_file = run_cmalign(fasta_file_path, cmfile_path, cpu_cores, progress_messages)
        progress_messages.append(f"Generated gzipped traceback file: {gz_traceback_file}")

        # 调用 make_onehot_of_cm_from_traceback 处理生成的 gz 文件
        progress_messages.append(f"Calling make_onehot_of_cm_from_traceback with: {gz_traceback_file}, {cmfile_path}")
        output_h5 = make_onehot_of_cm_from_traceback(gz_traceback_file, cmfile_path, progress_messages)
        progress_messages.append(f"Onehot file created: {output_h5}")

        # 上传生成的 h5 文件到 MinIO
        output_url = upload_to_minio(output_h5, user_id, progress_messages)

        # 返回处理结果
        return jsonify({"output_file": output_url, "progress_messages": progress_messages})

    except Exception as e:
        print(f"Error in handle_process_traceback: {str(e)}")
        return jsonify({"error": str(e)}), 500
