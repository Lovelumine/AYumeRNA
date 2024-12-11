import requests
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service as ChromeService
import time

# 设置 Selenium，无头浏览器模式
options = Options()
options.add_argument("--headless")  # 无头模式
options.add_argument("--disable-gpu")
options.add_argument("--no-sandbox")

# 替换为您实际的 ChromeDriver 可执行文件路径
service = ChromeService(executable_path='/usr/local/bin/chromedriver')  # 请确保路径正确
driver = webdriver.Chrome(service=service, options=options)

# 访问网站以获取 cookies
driver.get("https://lowelab.ucsc.edu/cgi-bin/tRNAscan-SE2.cgi")

# 等待页面完全加载
time.sleep(2)

# 从浏览器获取 cookies
cookies = driver.get_cookies()
driver.quit()

# 将 cookies 转换为适用于 requests 库的格式
session = requests.Session()
for cookie in cookies:
    session.cookies.set(cookie['name'], cookie['value'])

# 准备表单数据
payload = {
    'expect': '0.0000001',
    'qformat': 'raw',
    'database': 'all-trnas',
    'seqname': 'Your-seq-trna1',
    'gcode': 'Universal',
    'qseq': 'GGCCCAATAGCTAAGTAGGTATAGCAGGGGACTGAAAATCCCCGTGtCGGCAGTTCGATTCTGCCTTGGGCCA',
    'options': '',  # 如果有具体选项，请填写
}

# 准备文件参数，包含空的 'seqfile'，以及 'qseq' 序列
files = {
    'qseq': ('GGCCCAATAGCTAAGTAGGTATAGCAGGGGACTGAAAATCCCCGTGtCGGCAGTTCGATTCTGCCTTGGGCCA'),
    'seqfile': ('', '', 'application/octet-stream')
}

# 使用携带 cookies 的会话发送 POST 请求，禁用 SSL 验证
response = session.post(
    'https://lowelab.ucsc.edu/cgi-bin/tRNAscan-SE2.cgi',
    data=payload,
    files=files,
    verify=False  # 禁用 SSL 证书验证
)

# 打印响应的 HTML 内容
print(response.text)
