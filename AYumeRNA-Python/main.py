import os
import subprocess
import gzip
import requests
import tempfile
from flask import Flask, request, jsonify
from pathlib import Path
from scripts.make_onehot_from_traceback import make_onehot_of_cm_from_traceback
from minio import Minio
import datetime

app = Flask(__name__)

# MinIO 配置
MINIO_URL = 'http://minio.lumoxuan.cn'  # 替换为您的 MinIO 服务地址，确保包含协议（http:// 或 https://）
MINIO_ACCESS_KEY = 'uv9Ey4hCgAeF9US8IvW7'    # 替换为您的 MinIO Access Key
MINIO_SECRET_KEY = 'EJxNuc7hJId6cW969JXcMtd14xs7d9wTmC17Mn5W'    # 替换为您的 MinIO Secret Key
MINIO_BUCKET_NAME = 'ayumerna'          # 替换为您的 MinIO 存储桶名称

# 初始化 MinIO 客户端
minio_client = Minio(
    MINIO_URL.replace('http://', '').replace('https://', ''),  # 去掉协议部分
    access_key=MINIO_ACCESS_KEY,
    secret_key=MINIO_SECRET_KEY,
    secure=MINIO_URL.startswith('https://')
)

# 禁用代理（确保不使用系统配置的代理）
os.environ['NO_PROXY'] = '*'

# 用于从 MinIO 下载文件，忽略 SSL 证书验证
def download_file_from_minio(url, destination):
    try:
        print(f"Downloading file from {url} to {destination}...")
        response = requests.get(url, stream=True, verify=False, proxies={'http': None, 'https': None})  # 不使用代理
        if response.status_code == 200:
            with open(destination, 'wb') as f:
                f.write(response.content)
            print(f"Downloaded {url} to {destination}")
        else:
            raise Exception(f"Failed to download file from {url}, status code: {response.status_code}")
    except Exception as e:
        print(f"Error during download: {e}")
        raise

# 调用 cmalign 生成对齐文件和 traceback.txt
def run_cmalign(fasta_file, cmfile, cpu_cores=4, progress_messages=[]):
    try:
        # 生成相关文件的名称
        basename, _ = os.path.splitext(fasta_file)
        traceback_file = f"{basename}_notrunc_traceback.txt"
        gz_traceback_file = f"{traceback_file}.gz"
        score_file = f"{basename}_notrunc_score.txt"
        insertion_file = f"{basename}_notrunc_insertion.txt"
        elstate_file = f"{basename}_notrunc_ELstate.txt"
        outstk_tmp = f"{basename}_notrunc_tmp.sto"
        outstk = f"{basename}_notrunc.sto"

        # 调用 cmalign 生成 traceback 文件
        cmalign_cmd = (
            f"cmalign --cpu {cpu_cores} --notrunc --tfile {traceback_file} "
            f"--sfile {score_file} --ifile {insertion_file} --elfile {elstate_file} "
            f"{cmfile} {fasta_file} > {outstk_tmp}"
        )
        progress_messages.append(f"Running cmalign with command: {cmalign_cmd}")
        result = subprocess.run(cmalign_cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        progress_messages.append(f"cmalign output: {result.stdout.decode('utf-8', errors='replace')}")
        progress_messages.append(f"cmalign error (if any): {result.stderr.decode('utf-8', errors='replace')}")
        result.check_returncode()  # 检查命令是否执行成功

        # 压缩 traceback.txt 为 gzip 格式，强制覆盖已有文件 (-f)
        gzip_cmd = f"gzip -f {traceback_file}"
        progress_messages.append(f"Compressing {traceback_file} to {gz_traceback_file}")
        result = subprocess.run(gzip_cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        progress_messages.append(f"gzip output: {result.stdout.decode('utf-8', errors='replace')}")
        progress_messages.append(f"gzip error (if any): {result.stderr.decode('utf-8', errors='replace')}")
        result.check_returncode()

        # 使用 esl-reformat 进行 Stockholm 文件的重新格式化
        esl_reformat_cmd = f"esl-reformat --informat stockholm -o {outstk} stockholm {outstk_tmp}"
        progress_messages.append(f"Running esl-reformat with command: {esl_reformat_cmd}")
        result = subprocess.run(esl_reformat_cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        progress_messages.append(f"esl-reformat output: {result.stdout.decode('utf-8', errors='replace')}")
        progress_messages.append(f"esl-reformat error (if any): {result.stderr.decode('utf-8', errors='replace')}")
        result.check_returncode()

        # 删除临时的 sto 文件
        progress_messages.append(f"Removing temporary file: {outstk_tmp}")
        os.remove(outstk_tmp)

        return gz_traceback_file

    except Exception as e:
        progress_messages.append(f"Error during cmalign process: {str(e)}")
        raise

# 主处理逻辑
def process_traceback(fasta_file, cmfile, cpu_cores, user_id):
    progress_messages = []
    try:
        # 调用 cmalign 生成对齐和 traceback 文件
        gz_traceback_file = run_cmalign(fasta_file, cmfile, cpu_cores, progress_messages)
        progress_messages.append(f"Generated gzipped traceback file: {gz_traceback_file}")

        # 调用 make_onehot_of_cm_from_traceback 处理生成的 gz 文件
        progress_messages.append(f"Calling make_onehot_of_cm_from_traceback with: {gz_traceback_file}, {cmfile}")
        output_h5 = make_onehot_of_cm_from_traceback(gz_traceback_file, cmfile, progress_messages)
        progress_messages.append(f"Onehot file created: {output_h5}")

        # 上传生成的 h5 文件到 MinIO
        timestamp = datetime.datetime.now().strftime('%Y%m%d%H%M%S')
        object_name = f"{user_id}-{timestamp}-onehot.h5"

        # 确保存储桶存在
        found = minio_client.bucket_exists(MINIO_BUCKET_NAME)
        if not found:
            minio_client.make_bucket(MINIO_BUCKET_NAME)

        # 上传文件
        minio_client.fput_object(
            MINIO_BUCKET_NAME,
            object_name,
            output_h5,
            content_type='application/octet-stream'
        )

        minio_url = f"{MINIO_URL}/{MINIO_BUCKET_NAME}/{object_name}"
        progress_messages.append(f"Uploaded file to MinIO: {minio_url}")

        return minio_url, progress_messages

    except Exception as e:
        progress_messages.append(f"Error in processing traceback: {str(e)}")
        raise

# Flask 处理请求的部分：
@app.route('/process_traceback', methods=['POST'])
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
        output_url, progress_messages = process_traceback(fasta_file_path, cmfile_path, cpu_cores, user_id)

        # 返回处理结果
        return jsonify({"output_file": output_url, "progress_messages": progress_messages})

    except Exception as e:
        print(f"Error in handle_process_traceback: {str(e)}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=2002)