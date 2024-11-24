import random

def generate_sequence(length):
    """生成一个随机的 tRNA 序列（包括随机的碱基）"""
    bases = ['A', 'T', 'C', 'G']
    return ''.join(random.choice(bases) for _ in range(length))

def generate_aligned_sequences():
    """生成两个具有匹配区域的 tRNA 序列"""
    seq1 = generate_sequence(50)
    seq2 = generate_sequence(50)

    # 在 seq1 和 seq2 中创建一个匹配区域
    match_start = random.randint(10, 30)
    match_end = match_start + 10
    seq1 = seq1[:match_start] + seq2[match_start:match_end] + seq1[match_end:]

    # 另一处区域也做相似的处理
    second_match_start = random.randint(35, 45)
    second_match_end = second_match_start + 7
    seq1 = seq1[:second_match_start] + seq2[second_match_start:second_match_end] + seq1[second_match_end:]

    return seq1, seq2

# 生成示例序列
seq1, seq2 = generate_aligned_sequences()

# 打印生成的序列
print("Generated Sequence 1:", seq1)
print("Generated Sequence 2:", seq2)
