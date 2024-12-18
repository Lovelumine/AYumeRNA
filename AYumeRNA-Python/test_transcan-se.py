import requests
import json

# 定义服务器地址
url = "http://127.0.0.1:2002/run"

# 准备测试数据
data = {
    "sequence": "UGUAGGAUGGCGGAGUGGUUAACGCAUGCGCCUUUAAAGCGCAAGGUCCUGGGUUCGAAUCCCGGUCCUAUAA"
}

# 发送 POST 请求
response = requests.post(url, json=data)

# 检查响应状态
if response.status_code == 200:
    print("Request was successful!")
    # 打印返回的数据
    print("Response JSON:", json.dumps(response.json(), indent=4))
else:
    print(f"Request failed with status code {response.status_code}")
    print("Response:", response.text)