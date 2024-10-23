import os
import sys

sys.path.append("./src")
import preprocess

def fetch_seed_sequence_native(path_to_Rfamseed, rfam_acc, output_dir):
    # 从Rfam.seed中提取种子序列
    order_of_rfam_of_interest = 0
    with open(path_to_Rfamseed, "r", encoding="latin-1") as f:
        line = f.readline()
        while line:
            if line.startswith("#=GF AC"):
                id_ = line.split(" ")[-1].strip()  # 提取Rfam编号
                if id_ == rfam_acc:
                    break
                order_of_rfam_of_interest += 1
            line = f.readline()

    counter = 0
    sequences = []
    names = []
    with open(path_to_Rfamseed, "r", encoding="latin-1") as f:
        for line in f:
            if line.startswith("//"):  # 结束标记
                break
            elif line.startswith("#") or not line.strip():
                continue
            elif line.startswith(" "):  # 序列部分的连续行
                sequences[-1] += line.strip().replace(" ", "")
            else:  # 新的序列头或序列部分
                parts = line.strip().split()
                names.append(parts[0])
                sequences.append(parts[1])

    # 保存带有空位的种子序列为FASTA格式
    path_gapped = os.path.join(output_dir, f"{rfam_acc}_seed_gapped.fa")
    with open(path_gapped, "w") as f:
        for name, seq in zip(names, sequences):
            f.write(f">{name}\n{seq}\n")

    # 去除空位并保存为无间隙的种子序列
    path_ungapped = os.path.join(output_dir, f"{rfam_acc}_seed.fa")
    with open(path_ungapped, "w") as f:
        for name, seq in zip(names, sequences):
            ungapped_seq = seq.replace("-", "")
            f.write(f">{name}\n{ungapped_seq}\n")

    return path_ungapped


def remove_seed_from_full_native(fasta_seed, fasta_full):
    # 从全序列中移除与种子序列相同的序列
    seq_seed = set()
    with open(fasta_seed, "r") as file:
        seq = None
        for line in file:
            if line.startswith(">"):
                if seq:
                    seq_seed.add(seq)
                seq = ""
            else:
                seq += line.strip().upper().replace("T", "U")
        if seq:
            seq_seed.add(seq)

    # 读取全序列，并移除种子序列
    seq_all = set()
    records_seed_removed = []
    with open(fasta_full, "r") as full:
        seq = None
        header = None
        for line in full:
            if line.startswith(">"):
                if seq and header:
                    if seq not in seq_seed and set(seq) <= {"A", "C", "G", "U"}:
                        records_seed_removed.append((header, seq))
                    seq_all.add(seq)
                header = line.strip()
                seq = ""
            else:
                seq += line.strip().upper().replace("T", "U")
        if seq and header:
            if seq not in seq_seed and set(seq) <= {"A", "C", "G", "U"}:
                records_seed_removed.append((header, seq))
            seq_all.add(seq)

    # 保存移除种子序列后的结果
    fastaname_seed_removed = os.path.splitext(fasta_full)[0] + "_seed_removed.fa"
    with open(fastaname_seed_removed, "w") as output_file:
        for header, seq in records_seed_removed:
            output_file.write(f"{header}\n{seq}\n")

    print("种子序列数量\t:", len(seq_seed))
    print("所有序列数量\t:", len(seq_all))
    print("保留序列数量\t:", len(records_seed_removed))

    return fastaname_seed_removed


def main_native(rfam_acc, output_dir, path_to_Rfamseed, cpu=2):
    # 1. 去除重复序列
    fastaname_uniqued = preprocess.uniquenize(os.path.join(output_dir, f"{rfam_acc}.fa"))

    # 2. 提取种子序列并移除种子序列
    ungapped_seed = fetch_seed_sequence_native(path_to_Rfamseed, rfam_acc, output_dir)
    fastaname_uniques_seed_removed = remove_seed_from_full_native(ungapped_seed, fastaname_uniqued)

    return fastaname_uniques_seed_removed


if __name__ == '__main__':
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument('--seed_file', default='./datasets/Rfam.seed', help='Rfam种子文件路径')
    parser.add_argument('--rfam', help='Rfam编号', default="RF01317")
    parser.add_argument('--output_dir', default="datasets/ForFigure2", help='输出目录')
    parser.add_argument('--cpu', default=2, type=int, help='使用的CPU数量')

    args = parser.parse_args()

    main_native(args.rfam, args.output_dir, args.seed_file, args.cpu)
