import unittest
import requests
import json

class TestChargeRoute(unittest.TestCase):
    BASE_URL = "http://127.0.0.1:2002/charge"

    def test_valid_sequence(self):
        """
        测试一个有效的 tRNA 序列，检查 T-stem 提取是否正确。
        """
        sequence = "GGAAGUGAAGCUCAAUGGUAGAGCAGCGGACUUCAAAUCCGUCCGUUCUAGGUUCGACUCCUAGCACUUCCA"
        payload = {"sequence": sequence}

        response = requests.post(self.BASE_URL, json=payload)
        self.assertEqual(response.status_code, 200, "请求失败，返回状态码不为 200")

        data = response.json()
        print("Response:", data)

        self.assertIn("T-stem Sequence", data, "返回结果中缺少 T-stem Sequence 字段")
        self.assertIn("T-stem Position", data, "返回结果中缺少 T-stem Position 字段")

        # 额外验证 T-stem 内容是否符合预期格式
        tstem_sequence = data["T-stem Sequence"]
        tstem_position = data["T-stem Position"]

        print(f"T-stem Sequence: {tstem_sequence}")
        print(f"T-stem Position: {tstem_position}")

        self.assertIsInstance(tstem_sequence, str, "T-stem Sequence 应该是字符串")
        self.assertRegex(tstem_position, r"^\d+-\d+$", "T-stem Position 格式不正确")

    def test_invalid_sequence(self):
        """
        测试无效输入（空序列），检查是否返回错误信息。
        """
        payload = {"sequence": ""}

        response = requests.post(self.BASE_URL, json=payload)
        self.assertEqual(response.status_code, 400, "无效输入应返回状态码 400")

        data = response.json()
        print("Response for invalid input:", data)

        self.assertIn("error", data, "无效输入时应返回错误信息")

    def test_missing_sequence_key(self):
        """
        测试请求中缺少 sequence 键的情况。
        """
        payload = {}

        response = requests.post(self.BASE_URL, json=payload)
        self.assertEqual(response.status_code, 400, "缺少序列时应返回状态码 400")

        data = response.json()
        print("Response for missing sequence key:", data)

        self.assertIn("error", data, "缺少序列时应返回错误信息")

if __name__ == '__main__':
    unittest.main()