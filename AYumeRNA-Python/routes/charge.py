import os
import subprocess
import re
import uuid
from flask import Blueprint, request, jsonify

charge_bp = Blueprint('charge', __name__)

def run_trnascan(sequence: str, output_file: str, struct_file: str):
    """
    运行 tRNAscan-SE 命令，保存输入并生成输出和结构文件
    """
    try:
        # 生成唯一的输入文件名
        input_file = f"{uuid.uuid4()}_input.fasta"
        print(f"\n[INFO] Saving input sequence to file: {input_file}")
        with open(input_file, "w") as f:
            f.write(">input_sequence\n" + sequence)

        # 运行 tRNAscan-SE 命令
        cmd = f"tRNAscan-SE -A -o {output_file} -f {struct_file} {input_file}"
        print(f"\n[INFO] Running command:\n{cmd}")
        result = subprocess.run(cmd, shell=True, check=True, capture_output=True, text=True)

        print("\n[INFO] tRNAscan-SE Output:")
        print(result.stdout)
        if result.stderr:
            print("\n[WARNING] tRNAscan-SE Errors:")
            print(result.stderr)

        print(f"\n[INFO] tRNAscan-SE analysis completed successfully!\nOutput saved to: {output_file}\nStructure saved to: {struct_file}")

        # 删除临时输入文件
        os.remove(input_file)
        print(f"\n[INFO] Temporary input file '{input_file}' removed.\n")
    except subprocess.CalledProcessError as e:
        print("\n[ERROR] Error running tRNAscan-SE:")
        print(e.stderr)
        raise RuntimeError(f"tRNAscan-SE execution failed: {e.stderr}")

def extract_last_tstem_from_structure(struct_file: str):
    """
    解析结构文件，提取最后一个 T-stem，并确保匹配
    """
    sequence, structure = "", ""

    print(f"\n[INFO] Reading structure file: {struct_file}")
    with open(struct_file, "r") as f:
        for line in f:
            if line.startswith("Seq:"):
                sequence = line.split(":")[1].strip().replace(" ", "")
            elif line.startswith("Str:"):
                structure = line.split(":")[1].strip().replace(" ", "")

    print("\n[INFO] Full tRNA Sequence:")
    print(sequence)
    print("\n[INFO] Full Secondary Structure:")
    print(structure)

    if not sequence or not structure:
        raise ValueError("No sequence or structure found in the output file.")

    # 提取最后一个 T-stem 结构
    print("\n[INFO] Identifying T-stem structures in the secondary structure...")
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


        return {"tstem_sequence": f"{tstem_sequence}", "tstem_position": f"{tstem_start + 1}-{tstem_end}"}
    else:
        print("[ERROR] No T-stem structure found!")
        raise ValueError("No T-stem structure found!")

@charge_bp.route('/charge', methods=['POST'])
def charge_analysis():
    """
    接收用户的 tRNA 序列，运行 tRNAscan-SE 并提取最后的 T-stem
    """
    try:
        data = request.json
        sequence = data.get("sequence")
        if not sequence:
            print("\n[ERROR] No sequence provided in request.")
            return jsonify({"error": "No sequence provided"}), 400

        # 定义唯一文件名
        file_id = str(uuid.uuid4())
        output_file = f"{file_id}_output.txt"
        struct_file = f"{file_id}_structure.ct"

        print("\n=== Starting tRNAscan-SE Analysis Pipeline ===")
        print(f"[INFO] Input sequence: {sequence}")

        # 运行分析
        run_trnascan(sequence, output_file, struct_file)
        tstem_info = extract_last_tstem_from_structure(struct_file)

        # 清理临时文件
        os.remove(output_file)
        os.remove(struct_file)
        print(f"\n[INFO] Temporary output files '{output_file}' and '{struct_file}' removed.")

        return jsonify({
            "T-stem Sequence": tstem_info["tstem_sequence"],
            "T-stem Position": tstem_info["tstem_position"]
        })
    except Exception as e:
        print(f"\n[ERROR] Exception occurred: {str(e)}")
        return jsonify({"error": str(e)}), 500