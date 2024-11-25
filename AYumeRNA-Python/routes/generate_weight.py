import os
import tempfile
from flask import Blueprint, request, jsonify
from helpers import download_file_from_minio, upload_to_minio
import h5py
import numpy as np
import time
import random
from multiprocessing import Pool

# 创建 Blueprint
generate_weight_bp = Blueprint('generate_weight', __name__)

# 设置随机数种子
random.seed(42)

# 路由处理
@generate_weight_bp.route('/generate_weight', methods=['POST'])
def handle_generate_weight():
    try:
        # 获取请求的 JSON 数据
        data = request.get_json()
        print(f"Received data: {data}")

        # 从 JSON 中获取必要的参数
        file_url = data.get('file_url')
        user_id = data.get('user_id')
        mode = data.get('mode', 'cm')  # 默认模式为 'cm'
        threshold = data.get('threshold', 0.1)
        n_samples = data.get('n_samples', float('inf'))
        cpu = data.get('cpu', 4)
        print_every = data.get('print_every', 500)

        if not file_url or not user_id:
            return jsonify({"error": "Missing file_url or user_id"}), 400

        # 下载文件到临时目录
        temp_dir = tempfile.gettempdir()
        input_file_path = os.path.join(temp_dir, "input_weight.h5")
        download_file_from_minio(file_url, input_file_path)

        # 生成输出文件路径
        output_file_path = os.path.join(temp_dir, "output_weight.h5")

        # 调用权重计算函数
        Ntotal, Neff = compute_and_write_weight(
            X_fname=input_file_path,
            threshold=threshold,
            outfile=output_file_path,
            mode=mode,
            sampling_threshold=n_samples,
            cpu=cpu,
            print_every=print_every
        )

        # 上传生成的权重文件到 MinIO
        progress_messages = []
        output_url = upload_to_minio(output_file_path, user_id, file_type='weight', progress_messages=progress_messages)

        # 删除临时文件
        os.remove(input_file_path)
        os.remove(output_file_path)

        # 返回处理结果
        return jsonify({
            "output_url": output_url,
            "Ntotal": Ntotal,
            "Neff": Neff,
            "progress_messages": progress_messages
        })

    except Exception as e:
        print(f"Error in handle_generate_weight: {str(e)}")
        return jsonify({"error": str(e)}), 500

# 以下是您提供的权重计算代码的函数部分

def load_data_cm(path):
    """
    加载 'cm' 模式的数据
    返回值：(tr, s, p)
    """
    data = h5py.File(path, "r")
    tr = np.nan_to_num(data["tr"][:], copy=False).transpose(0, -1, -2)
    s = data["s"][:].transpose(0, -1, -2)
    p = data["p"][:].transpose(0, -1, -2)
    data.close()
    return tr, s, p

def load_data_cg(path):
    """
    加载 'cg' 模式的数据
    返回值：onehot
    """
    data = h5py.File(path, "r")
    onehot = data["data"][:].transpose(0, -1, -2)
    data.close()
    return onehot

def calc_distance_cm(trsp_i, trsp_j):
    (tr_i, s_i, p_i), (tr_j, s_j, p_j) = trsp_i, trsp_j
    distance = np.abs(tr_i - tr_j).sum() + np.abs(s_i - s_j).sum() + np.abs(p_i - p_j).sum()
    return distance

def calc_distance_cg(xi, xj):
    distance = np.abs(xi - xj).sum()
    return distance

def compute_and_write_weight(X_fname, threshold, outfile, mode="cm", sampling_threshold=10000, sample_ratio_over_threshold=0.05, cpu=4, print_every=500):
    if mode not in {"cm", "c", "g"}:
        raise ValueError("Select mode from c/g/cm")
    
    if mode == "cm":
        tr_train, s_train, p_train = load_data_cm(X_fname)
        N_COLLUMNS = tr_train.shape[-1] + s_train.shape[-1] + p_train.shape[-1]
        Ntotal = tr_train.shape[0]
        if Ntotal < sampling_threshold:
            n_samples = Ntotal
        else:
            n_samples = int(Ntotal * sample_ratio_over_threshold)
            
        print("CM mode.")
        print(f"Sampled size: {str(n_samples)}")
        print(f"N_COLUMNS: {str(N_COLLUMNS)}")
        print(f"*"*50)
        Neff = 0
        with h5py.File(outfile, "w") as f:
            f.create_dataset('weight', (Ntotal, ))
            start = time.time()
            for i in range(Ntotal):
                tmp_index = set(range(Ntotal))
                tmp_index.remove(i)
                sampled_index = random.sample(tmp_index, k=int(n_samples -1))
                tr_i, s_i, p_i = tr_train[i], s_train[i], p_train[i]
                onehot_pairs = [((tr_i, s_i, p_i), (tr_train[j], s_train[j], p_train[j])) for j in sampled_index]

                with Pool(cpu) as p:  
                    distances = p.starmap(calc_distance_cm, onehot_pairs)

                n_neighbor = 1 + sum([1 if d < N_COLLUMNS * threshold else 0 for d in distances])
                weight = 1 / n_neighbor
                f["weight"][i] = weight
                Neff += weight
                
                if i % print_every == 0 and i > 0:
                    finish = time.time()
                    est_time = (Ntotal - i) * (finish - start) / print_every
                    print(f"{i}/{Ntotal}\t, sampling {n_samples}, estimated remaining time: {est_time} sec.")
                    start = time.time()

    elif mode in {"c", "g"}:
        onehot = load_data_cg(X_fname)
        N_COLLUMNS = onehot.shape[-1]
        Ntotal = onehot.shape[0]
        if Ntotal < sampling_threshold:
            n_samples = Ntotal
        else:
            n_samples = int(Ntotal * sample_ratio_over_threshold)
        print(f"Char/Gram mode.")
        print(f"Sampled size: {n_samples}")
        print(f"N_COLUMNS: {N_COLLUMNS}")
        print(f"*"*50)
        Neff = 0
        with h5py.File(outfile, "w") as f:
            f.create_dataset('weight', (Ntotal, ))
            start = time.time()
            for i in range(Ntotal):
                tmp_index = set(range(Ntotal))
                tmp_index.remove(i)
                sampled_index = random.sample(tmp_index, k=int(n_samples -1))
                x_i = onehot[i]
                onehot_pairs = [(x_i, onehot[j]) for j in sampled_index]

                with Pool(cpu) as p:
                    distances = p.starmap(calc_distance_cg, onehot_pairs)

                n_neighbor = 1 + sum([1 if d < N_COLLUMNS * threshold else 0 for d in distances])
                weight = 1 / n_neighbor
                f["weight"][i] = weight
                Neff += weight
                if i % print_every == 0 and i > 0:
                    finish = time.time()
                    est_time = (Ntotal - i) * (finish - start) / print_every
                    print(f"{i}/{Ntotal}\t, sampling {n_samples}, estimated remaining time: {est_time} sec.")
                    start = time.time()

    return Ntotal, Neff
