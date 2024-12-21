import subprocess
import os
import re

def run_trnascan(sequence: str, output_file: str, struct_file: str):
    """
    运行 tRNAscan-SE，将序列保存到文件，然后分析结构
    """
    print("Step 1: Saving input sequence to temporary file...")
    input_file = "temp_input.fasta"
    with open(input_file, "w") as f:
        f.write(">input_sequence\n" + sequence)
    print(f"Sequence saved to {input_file}\n")

    print("Step 2: Running tRNAscan-SE analysis...")
    # 运行 tRNAscan-SE，并使用 -o 和 -f 参数输出结构和详细结果
    cmd = f"tRNAscan-SE -A -o {output_file} -f {struct_file} {input_file}"
    try:
        subprocess.run(cmd, shell=True, check=True)
        print(f"tRNAscan-SE analysis completed successfully!\nResults saved to: {output_file}\nStructure saved to: {struct_file}\n")
    except subprocess.CalledProcessError as e:
        print("Error running tRNAscan-SE:")
        print(e)
        exit(1)
    finally:
        # 删除临时输入文件
        if os.path.exists(input_file):
            os.remove(input_file)
            print(f"Temporary input file {input_file} removed.\n")

def extract_last_tstem_from_structure(struct_file: str):
    """
    解析 tRNAscan-SE 结果文件，提取最后一个 T-stem 区域的结构和序列，确保数量匹配
    """
    print("Step 3: Extracting sequence and structure from tRNAscan-SE output...")
    sequence = ""
    structure = ""

    # 读取结构文件内容
    with open(struct_file, "r") as f:
        for line in f:
            if line.startswith("Seq:"):
                sequence = line.split(":")[1].strip().replace(" ", "")
            elif line.startswith("Str:"):
                structure = line.split(":")[1].strip().replace(" ", "")

    if not sequence or not structure:
        raise ValueError("Error: Sequence or structure not found in the file.")

    print("Full tRNA Sequence:")
    print(sequence)
    print("\nFull Secondary Structure:")
    print(structure)

    # 使用正则表达式找到所有 >>>>>.......<<<<< 结构
    print("\nStep 4: Identifying T-stem structures in the secondary structure...")
    tstem_matches = [match for match in re.finditer(r'(>+\.{0,10}<+)', structure)]

    if tstem_matches:
        # 选择最后一个匹配的结构
        print(f"Found {len(tstem_matches)} potential T-stem structures.")
        last_match = tstem_matches[-1]
        tstem_structure = last_match.group()

        # 计算 > 和 < 数量差
        num_gt = tstem_structure.count('>')
        num_lt = tstem_structure.count('<')
        diff = abs(num_gt - num_lt)

        # 截断多余的 <
        if num_gt < num_lt:
            tstem_structure = tstem_structure[:num_gt + (tstem_structure.count('.') if '.' in tstem_structure else 0) + num_gt]

        tstem_start = last_match.start()
        tstem_end = tstem_start + len(tstem_structure) - diff  # 更新结束位置
        tstem_sequence = sequence[tstem_start:tstem_end]

        print("\nIdentified T-stem:")
        print(f"Structure: {tstem_structure}")
        print(f"Sequence: {tstem_sequence}")
        print(f"Position: {tstem_start + 1}-{tstem_end}")
    else:
        print("Error: No T-stem structure found in the secondary structure.")

def main():
    # 输入 tRNA 序列
    sequence = "GGAAGUGAAGCUCAAUGGUAGAGCAGCGGACUUCAAAUCCGUCCGUUCUAGGUUCGACUCCUAGCACUUCCA"

    # 定义输出文件
    output_file = "trnascan_output.txt"
    struct_file = "trnascan_structure.ct"

    print("\n=== tRNAscan-SE Analysis Pipeline ===\n")
    try:
        # 运行 tRNAscan-SE
        run_trnascan(sequence, output_file, struct_file)

        # 提取最后一个 T-stem
        extract_last_tstem_from_structure(struct_file)
    except Exception as e:
        print(f"\nError: {str(e)}")
    finally:
        # 清理临时输出文件
        if os.path.exists(output_file):
            os.remove(output_file)
            print(f"\nTemporary file {output_file} removed.")
        if os.path.exists(struct_file):
            os.remove(struct_file)
            print(f"Temporary file {struct_file} removed.")

if __name__ == "__main__":
    main()