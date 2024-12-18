import subprocess
import os
import time

def run_trnascan_se(sequence, output_dir):
    # 生成时间戳以创建唯一的文件名
    timestamp = time.strftime("%Y%m%d_%H%M%S")
    temp_dir = "temp_files"
    os.makedirs(temp_dir, exist_ok=True)
    
    # 临时文件路径
    temp_fasta_file = os.path.join(temp_dir, f"temp_{timestamp}.fasta")
    temp_output_file = os.path.join(temp_dir, f"trnascan_output_{timestamp}.txt")
    
    # 将序列写入临时文件
    with open(temp_fasta_file, 'w') as f_out:
        f_out.write(f">sequence_{timestamp}\n{sequence}\n")
    
    # 运行tRNAscan-SE命令（去掉不必要的 "A" 参数）
    command = [
        "tRNAscan-SE",  # tRNAscan-SE 的安装路径
        "-o", temp_output_file,  # 输出文件路径
        temp_fasta_file  # 输入的FASTA文件路径
    ]
    
    # 执行命令
    try:
        subprocess.run(command, check=True)
        print(f"tRNAscan-SE finished. Results saved in {temp_output_file}")
    except subprocess.CalledProcessError as e:
        print(f"Error occurred: {e}")
    
    # 删除临时文件
    os.remove(temp_fasta_file)
    
    return temp_output_file

def parse_trnascan_output(output_file):
    # 解析tRNAscan-SE的输出文件，获取最后一行数据
    tRNA_data = []
    
    try:
        with open(output_file, 'r') as file:
            lines = file.readlines()
            # 只处理最后一行
            last_line = lines[-1]
            columns = last_line.strip().split("\t")
            if len(columns) >= 9:  # 确保有足够的列
                tRNA = {
                    'tRNA Begin': columns[2],   # Start position (Begin)
                    'tRNA End': columns[3],     # End position (End)
                    'tRNA Type': columns[4],    # tRNA Type
                    'Anticodon': columns[5],    # Codon
                    'Infernal Score': columns[8]  # Score
                }
                tRNA_data.append(tRNA)
        return tRNA_data
    except Exception as e:
        print(f"Error parsing output file: {e}")
        return []

if __name__ == "__main__":
    # 通过函数获取序列
    sequence = "UGUAGGAUGGCGGAGUGGUUAACGCAUGCGCCUUUAAAGCGCAAGGUCCUGGGUUCGAAUCCCGGUCCUAUAA"  # 示例序列
    
    # 输出文件夹路径（确保这个目录存在）
    output_dir = "temp_files"
    
    # 运行tRNAscan-SE并获取输出文件
    output_file = run_trnascan_se(sequence, output_dir)
    
    # 解析输出文件并获取最后一行
    tRNA_data = parse_trnascan_output(output_file)
    
    # 打印解析结果
    if tRNA_data:
        for tRNA in tRNA_data:
            print(tRNA)
    else:
        print("No tRNA data found.")
    
    # 删除所有临时文件
    os.remove(output_file)