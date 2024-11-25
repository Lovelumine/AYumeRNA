import os
import tempfile
from flask import Blueprint, request, jsonify
from helpers import download_file_from_minio, upload_to_minio
import h5py
from sklearn.model_selection import train_test_split

# 创建 Blueprint
split_onehot_bp = Blueprint('split_onehot', __name__)

@split_onehot_bp.route('/process_split_onehot', methods=['POST'])
def handle_process_split_onehot():
    try:
        # 获取请求的 JSON 数据
        data = request.get_json()
        print(f"Received data: {data}")

        # 从 JSON 中获取文件 URL 和用户 ID
        file_url = data.get('file_url')
        user_id = data.get('user_id')
        train_ratio = data.get('train_ratio', 0.7)
        random_state = data.get('random_state', 42)

        if not file_url or not user_id:
            return jsonify({"error": "Missing file_url or user_id"}), 400

        # 下载文件到临时目录
        temp_dir = tempfile.gettempdir()
        input_file_path = os.path.join(temp_dir, "input_file.h5")
        download_file_from_minio(file_url, input_file_path)

        # 执行数据拆分
        train_file, valid_file, test_file = split_onehot_cm(input_file_path, train_ratio, random_state=random_state)

        # Initialize progress messages list
        progress_messages = []

        # 上传拆分后的文件到 MinIO
        train_url = upload_to_minio(train_file, user_id, "train", progress_messages=progress_messages)
        valid_url = upload_to_minio(valid_file, user_id, "valid", progress_messages=progress_messages)
        test_url = upload_to_minio(test_file, user_id, "test", progress_messages=progress_messages)

        # 删除临时文件
        os.remove(train_file)
        os.remove(valid_file)
        os.remove(test_file)
        os.remove(input_file_path)

        # 返回处理结果
        return jsonify({
            "train_url": train_url,
            "valid_url": valid_url,
            "test_url": test_url,
            "progress_messages": progress_messages
        })

    except Exception as e:
        print(f"Error in handle_process_split_onehot: {str(e)}")
        return jsonify({"error": str(e)}), 500

def split_onehot_cm(path_to_cmonehot, train_ratio=0.7, suffix="", random_state=42):
    """
    train/valid/test splitter for datasets of CM-VAE.
    """
    h5 = h5py.File(path_to_cmonehot, "r")
    ids = h5["id"][:]
    tr = h5["tr"][:]
    s = h5["s"][:]
    p = h5["p"][:]
    h5.close()

    id_train, id_vt, tr_train, tr_vt, s_train, s_vt, p_train, p_vt = train_test_split(
        ids, tr, s, p, test_size=1-train_ratio, random_state=random_state)
    id_valid, id_test, tr_valid, tr_test, s_valid, s_test, p_valid, p_test = train_test_split(
        id_vt, tr_vt, s_vt, p_vt, test_size=0.5, random_state=random_state)

    # 临时文件路径
    temp_dir = tempfile.gettempdir()
    train_file = os.path.join(temp_dir, "train.h5")
    valid_file = os.path.join(temp_dir, "valid.h5")
    test_file = os.path.join(temp_dir, "test.h5")

    with h5py.File(train_file, "w") as h5f:
        h5f.create_dataset("id", data=id_train)
        h5f.create_dataset("tr", data=tr_train)
        h5f.create_dataset("s", data=s_train)
        h5f.create_dataset("p", data=p_train)

    with h5py.File(valid_file, "w") as h5f:
        h5f.create_dataset("id", data=id_valid)
        h5f.create_dataset("tr", data=tr_valid)
        h5f.create_dataset("s", data=s_valid)
        h5f.create_dataset("p", data=p_valid)

    with h5py.File(test_file, "w") as h5f:
        h5f.create_dataset("id", data=id_test)
        h5f.create_dataset("tr", data=tr_test)
        h5f.create_dataset("s", data=s_test)
        h5f.create_dataset("p", data=p_test)

    return train_file, valid_file, test_file
