# routes/train.py

import os
import tempfile
from flask import Blueprint, request, jsonify
from helpers import download_file_from_minio, upload_to_minio
import scripts.train  # 导入训练模块
import sys
sys.path.append('./scripts')  # 确保可以找到 scripts.train 模块

train_bp = Blueprint('train', __name__)

@train_bp.route('/train', methods=['POST'])
def handle_train():
    try:
        # 获取请求的 JSON 数据
        data = request.get_json()
        print(f"Received data: {data}")

        # 从 JSON 中获取参数
        x_train_url = data.get('X_train_url')
        w_train_url = data.get('w_train_url')
        x_valid_url = data.get('X_valid_url')
        w_valid_url = data.get('w_valid_url')
        beta = float(data.get('beta', 0.001))  # 增加对 beta 参数的支持
        user_id = str(data.get('user_id', 'unknown_user'))
        other_args = data.get('other_args', {})
        progress_messages = []

        # 检查必要的参数
        if not x_train_url:
            return jsonify({"error": "Missing X_train_url"}), 400

        # 使用固定目录，避免临时目录被删除导致文件丢失
        data_dir_path = os.path.join(os.getcwd(), 'data')
        os.makedirs(data_dir_path, exist_ok=True)

        x_train_path = os.path.join(data_dir_path, 'X_train.h5')
        x_valid_path = os.path.join(data_dir_path, 'X_valid.h5')

        # 下载文件
        download_file_from_minio(x_train_url, x_train_path)
        progress_messages.append(f"Downloaded X_train to {x_train_path}")

        if x_valid_url:
            download_file_from_minio(x_valid_url, x_valid_path)
            progress_messages.append(f"Downloaded X_valid to {x_valid_path}")
        else:
            x_valid_path = None  # 如果没有提供，则设置为 None

        # 设置参数
        args = {
            'data_dir': data_dir_path,
            'X_train': os.path.basename(x_train_path),
            'beta': beta,
            'log': True,
            'log_dir': os.path.join(data_dir_path, 'logs'),
        }

        # 添加其他参数，如果未提供则使用默认值
        default_args = {
            'hidden': 128,
            'z_dim': 16,
            'stride': 1,
            'ker1': 5,
            'ch1': 5,
            'ker2': 5,
            'ch2': 5,
            'ker3': 7,
            'ch3': 8,
            'anneal_saturate_rate': 0.4,
            'anneal_rate': 1,
            'batch_size': 8,
            'epoch': 10,
            'learning_rate': 1e-3,
            'clip': 20,
            'random_seed': 42,
            'print_every': 20,
            'use_anneal': False,
            'only_training': False,
            'use_early_stopping': False,
            'tolerance': 3,
            'save_ckpt': False,
            'ckpt_iter': 3,
            'suffix': ''
        }

        # 合并参数
        for key, default_value in default_args.items():
            args[key] = other_args.get(key, default_value)

        # 确保日志目录存在
        os.makedirs(args['log_dir'], exist_ok=True)

        if w_train_url:
            w_train_path = os.path.join(data_dir_path, 'w_train.h5')
            download_file_from_minio(w_train_url, w_train_path)
            progress_messages.append(f"Downloaded w_train to {w_train_path}")
            args['w_train'] = os.path.basename(w_train_path)
        else:
            args['w_train'] = ''

        if x_valid_url:
            args['X_valid'] = os.path.basename(x_valid_path)
        else:
            args['X_valid'] = ''

        if w_valid_url:
            w_valid_path = os.path.join(data_dir_path, 'w_valid.h5')
            download_file_from_minio(w_valid_url, w_valid_path)
            progress_messages.append(f"Downloaded w_valid to {w_valid_path}")
            args['w_valid'] = os.path.basename(w_valid_path)
        else:
            args['w_valid'] = ''

        # 运行训练函数
        scripts.train.train_main(args)

        # 假设训练生成的模型保存在指定目录下
        model_output_path = os.path.join(args['log_dir'], f'model_epoch{args["epoch"]}{args.get("suffix", "")}.pt')

        if os.path.exists(model_output_path):
            # 上传模型到 MinIO
            output_url = upload_to_minio(model_output_path, user_id, file_type='model', progress_messages=progress_messages)
        else:
            raise Exception("Model output file not found.")

        # 返回结果
        return jsonify({"output_file": output_url, "progress_messages": progress_messages})

    except Exception as e:
        print(f"Error in handle_train: {str(e)}")
        # 将 progress_messages 添加到控制台输出
        for message in progress_messages:
            print(message)
        return jsonify({"error": str(e), "progress_messages": progress_messages}), 500
