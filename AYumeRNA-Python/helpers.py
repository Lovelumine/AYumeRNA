import os
import subprocess
import gzip
import requests
import tempfile
from flask import Flask, jsonify
from minio import Minio
import datetime

# 初始化 Flask 应用
app = Flask(__name__)

# MinIO 配置
MINIO_URL = 'http://minio.lumoxuan.cn'
MINIO_ACCESS_KEY = 'uv9Ey4hCgAeF9US8IvW7'
MINIO_SECRET_KEY = 'EJxNuc7hJId6cW969JXcMtd14xs7d9wTmC17Mn5W'
MINIO_BUCKET_NAME = 'ayumerna'

# 初始化 MinIO 客户端
minio_client = Minio(
    MINIO_URL.replace('http://', '').replace('https://', ''),  # 去掉协议部分
    access_key=MINIO_ACCESS_KEY,
    secret_key=MINIO_SECRET_KEY,
    secure=MINIO_URL.startswith('https://')
)

# 禁用代理
os.environ['NO_PROXY'] = '*'

# 通用函数：用于从 MinIO 下载文件
def download_file_from_minio(url, destination):
    try:
        print(f"Downloading file from {url} to {destination}...")
        response = requests.get(url, stream=True, verify=False, proxies={'http': None, 'https': None})
        if response.status_code == 200:
            with open(destination, 'wb') as f:
                f.write(response.content)
            print(f"Downloaded {url} to {destination}")
        else:
            raise Exception(f"Failed to download file from {url}, status code: {response.status_code}")
    except Exception as e:
        print(f"Error during download: {e}")
        raise

# 通用函数：运行 cmalign 工具并处理输出
def run_cmalign(fasta_file, cmfile, cpu_cores=4, progress_messages=[]):
    try:
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
        result.check_returncode()

        # 压缩 traceback.txt 为 gzip 格式
        gzip_cmd = f"gzip -f {traceback_file}"
        progress_messages.append(f"Compressing {traceback_file} to {gz_traceback_file}")
        result = subprocess.run(gzip_cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        progress_messages.append(f"gzip output: {result.stdout.decode('utf-8', errors='replace')}")
        progress_messages.append(f"gzip error (if any): {result.stderr.decode('utf-8', errors='replace')}")
        result.check_returncode()

        # 使用 esl-reformat 重新格式化文件
        esl_reformat_cmd = f"esl-reformat --informat stockholm -o {outstk} stockholm {outstk_tmp}"
        progress_messages.append(f"Running esl-reformat with command: {esl_reformat_cmd}")
        result = subprocess.run(esl_reformat_cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        progress_messages.append(f"esl-reformat output: {result.stdout.decode('utf-8', errors='replace')}")
        progress_messages.append(f"esl-reformat error (if any): {result.stderr.decode('utf-8', errors='replace')}")
        result.check_returncode()

        # 删除临时文件
        progress_messages.append(f"Removing temporary file: {outstk_tmp}")
        os.remove(outstk_tmp)

        return gz_traceback_file

    except Exception as e:
        progress_messages.append(f"Error during cmalign process: {str(e)}")
        raise

# 通用函数：上传文件到 MinIO
def upload_to_minio(local_file_path, user_id, progress_messages=[]):
    try:
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
            local_file_path,
            content_type='application/octet-stream'
        )

        minio_url = f"{MINIO_URL}/{MINIO_BUCKET_NAME}/{object_name}"
        progress_messages.append(f"Uploaded file to MinIO: {minio_url}")
        return minio_url

    except Exception as e:
        progress_messages.append(f"Error uploading file to MinIO: {str(e)}")
        raise
