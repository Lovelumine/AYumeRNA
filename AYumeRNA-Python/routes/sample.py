import os
import tempfile
from flask import Blueprint, request, jsonify
from helpers import download_file_from_minio, upload_to_minio
from scripts.sampling_from_gauss import sampling_CMVAE, helper_sampling_CMVAE  # 导入采样模块中的函数
from util import load_config
from models.CMVAE import CovarianceModelVAE
from infernal_tools import CMReader
import torch

sample_bp = Blueprint('sample', __name__)

@sample_bp.route('/sample', methods=['POST'])
def handle_sample():
    try:
        # 获取请求的 JSON 数据
        data = request.get_json()
        print(f"Received data: {data}")

        # 从 JSON 中获取参数
        config_url = data.get('config_url')
        ckpt_url = data.get('ckpt_url')
        cmfile_url = data.get('cmfile_url')
        n_samples = int(data.get('n_samples') or data.get('nSamples') or data.get('NSamples') or 100)
        user_id = str(data.get('user_id', 'unknown_user'))
        progress_messages = []

        # 检查必要的参数
        if not config_url or not ckpt_url or not cmfile_url:
            return jsonify({"error": "Missing required URL parameter(s)"}), 400

        # 使用固定目录，避免临时目录被删除导致文件丢失
        data_dir_path = os.path.join(os.getcwd(), 'data')
        os.makedirs(data_dir_path, exist_ok=True)

        # 下载 config、ckpt、cmfile 文件
        config_path = os.path.join(data_dir_path, 'config.yaml')
        ckpt_path = os.path.join(data_dir_path, 'model.pt')
        cmfile_path = os.path.join(data_dir_path, 'model.cm')

        download_file_from_minio(config_url, config_path)
        progress_messages.append(f"Downloaded config to {config_path}")
        download_file_from_minio(ckpt_url, ckpt_path)
        progress_messages.append(f"Downloaded checkpoint to {ckpt_path}")
        download_file_from_minio(cmfile_url, cmfile_path)
        progress_messages.append(f"Downloaded CM file to {cmfile_path}")

        # 加载配置和模型
        cfg = load_config(config_path)
        model = CovarianceModelVAE.build_from_config(config_path)
        model.load_model_from_ckpt(ckpt_path)
        model.to(model.device)

        # 加载 CM 文件并生成派生字典
        cmreader = CMReader(cmfile_path)
        progress_messages.append("Loading cm derivation dictionary.")
        cm_deriv_dict = cmreader.load_derivation_dict_from_cmfile()

        # 生成序列样本
        seq_sampled = sampling_CMVAE(model, cm_deriv_dict, cfg["Z_DIM"], n_samples)
        sampled_fasta_path = os.path.join(data_dir_path, 'sampled_sequences.fa')

        # 将序列写入到 Fasta 文件
        with open(sampled_fasta_path, "w") as f:
            for i, seq in enumerate(set(seq_sampled)):
                f.write(f">seq{str(i)}\n")
                f.write(f"{str(seq)}\n")
        progress_messages.append(f"Saved sampled sequences to {sampled_fasta_path}")

        # 上传 Fasta 文件到 MinIO
        output_url = upload_to_minio(sampled_fasta_path, user_id, file_type='fasta', progress_messages=progress_messages)

        # 替换返回的 URL
        adjusted_url = output_url.replace("http://127.0.0.1:9000", "https://minio.lumoxuan.cn")

        # 返回结果
        return jsonify({"output_file": adjusted_url, "progress_messages": progress_messages})

    except Exception as e:
        print(f"Error in handle_sample: {str(e)}")
        for message in progress_messages:
            print(message)
        return jsonify({"error": str(e), "progress_messages": progress_messages}), 500