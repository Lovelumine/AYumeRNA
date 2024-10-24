import os
import subprocess
import gzip
import requests
import tempfile
from flask import Flask, request, jsonify
from pathlib import Path
from scripts.make_onehot_from_traceback import make_onehot_of_cm_from_traceback

app = Flask(__name__)

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

# 自定义路径转换函数
def convert_to_wsl_path(win_path):
    try:
        # 替换盘符 (C: -> /mnt/c)
        if win_path[1:3] == ':\\' or win_path[1:3] == ':/':
            drive_letter = win_path[0].lower()
            # 将反斜杠替换为正斜杠
            win_path = win_path.replace('\\', '/')
            wsl_path = f"/mnt/{drive_letter}{win_path[2:]}"
        else:
            # 如果不是绝对路径，仅替换反斜杠为正斜杠
            wsl_path = win_path.replace('\\', '/')
        
        print(f"Converted {win_path} to WSL path: {wsl_path}")
        return wsl_path
    except Exception as e:
        print(f"Error in manual path conversion: {e}")
        raise

# 调用 WSL 中的 cmalign 生成对齐文件和 traceback.txt
def run_cmalign(fasta_file, cmfile, cpu_cores=4):
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

        # 将 Windows 路径转换为 WSL 路径
        wsl_fasta_file = convert_to_wsl_path(fasta_file)
        wsl_cmfile = convert_to_wsl_path(cmfile)
        wsl_traceback_file = convert_to_wsl_path(traceback_file)
        wsl_score_file = convert_to_wsl_path(score_file)
        wsl_insertion_file = convert_to_wsl_path(insertion_file)
        wsl_elstate_file = convert_to_wsl_path(elstate_file)
        wsl_outstk_tmp = convert_to_wsl_path(outstk_tmp)
        wsl_outstk = convert_to_wsl_path(outstk)

        # 调用 cmalign 生成 traceback 文件
        cmalign_cmd = (
            f"wsl cmalign --cpu {cpu_cores} --notrunc --tfile {wsl_traceback_file} "
            f"--sfile {wsl_score_file} --ifile {wsl_insertion_file} --elfile {wsl_elstate_file} "
            f"{wsl_cmfile} {wsl_fasta_file} > {wsl_outstk_tmp}"
        )
        print(f"Running cmalign with command: {cmalign_cmd}")
        result = subprocess.run(cmalign_cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        # 使用 errors='replace' 忽略解码错误
        print(f"cmalign output: {result.stdout.decode('utf-8', errors='replace')}")
        print(f"cmalign error (if any): {result.stderr.decode('utf-8', errors='replace')}")
        result.check_returncode()  # 检查命令是否执行成功

        # 压缩 traceback.txt 为 gzip 格式，强制覆盖已有文件 (-f)
        print(f"Compressing {wsl_traceback_file} to {gz_traceback_file}")
        result = subprocess.run(f"wsl gzip -f {wsl_traceback_file}", shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        print(f"gzip output: {result.stdout.decode('utf-8', errors='replace')}")
        print(f"gzip error (if any): {result.stderr.decode('utf-8', errors='replace')}")
        result.check_returncode()

        # 使用 esl-reformat 进行 Stockholm 文件的重新格式化
        esl_reformat_cmd = f"wsl /home/lovelumine/bin/esl-reformat --informat stockholm -o {wsl_outstk} stockholm {wsl_outstk_tmp}"
        print(f"Running esl-reformat with command: {esl_reformat_cmd}")
        result = subprocess.run(esl_reformat_cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        print(f"esl-reformat output: {result.stdout.decode('utf-8', errors='replace')}")
        print(f"esl-reformat error (if any): {result.stderr.decode('utf-8', errors='replace')}")
        result.check_returncode()

        # 删除临时的 sto 文件
        print(f"Removing temporary file: {wsl_outstk_tmp}")
        subprocess.run(f"wsl rm {wsl_outstk_tmp}", shell=True)

        return gz_traceback_file

    except Exception as e:
        print(f"Error during cmalign process: {str(e)}")
        raise

# 主处理逻辑
def process_traceback(fasta_file, cmfile, cpu_cores):
    try:
        # 调用 cmalign 生成对齐和 traceback 文件
        gz_traceback_file = run_cmalign(fasta_file, cmfile, cpu_cores)
        print(f"Generated gzipped traceback file: {gz_traceback_file}")

        # 调用 make_onehot_of_cm_from_traceback 处理生成的 gz 文件
        print(f"Calling make_onehot_of_cm_from_traceback with: {gz_traceback_file}, {cmfile}")
        output_h5 = make_onehot_of_cm_from_traceback(gz_traceback_file, cmfile)
        print(f"Onehot file created: {output_h5}")

        return output_h5

    except Exception as e:
        print(f"Error in processing traceback: {str(e)}")
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
        output_h5 = process_traceback(fasta_file_path, cmfile_path, cpu_cores)

        # 返回处理结果
        return jsonify({"output_file": output_h5})

    except Exception as e:
        print(f"Error in handle_process_traceback: {str(e)}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=2002)
