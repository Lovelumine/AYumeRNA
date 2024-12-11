import requests
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service as ChromeService
import time

# 设置 Selenium，无头浏览器模式
options = Options()
options.add_argument("--headless")
options.add_argument("--disable-gpu")
options.add_argument("--no-sandbox")

# 替换为您实际的 ChromeDriver 可执行文件路径
service = ChromeService(executable_path='path_to_chromedriver')  # 请将此路径替换为实际路径
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

# 准备带有指定参数的负载
payload = {
    'result': 'html',
    'organism': 'Eukaryotic',
    'mode': 'Default',
    'qformat': 'raw',
    'seqname': '',
    'qseq': 'GGCCCAAUAGCUAAGUAGGUAUAGCAGGGGACUGAAAAUCCCCGUGUCGGCAGUUCGAUUCUGCCUUGGGCCA',
    'gcode': 'Universal',
    'score': ''
}

# 准备文件参数，包含一个空的 'seqfile'
files = {
    'seqfile': ('', '', 'application/octet-stream')
}

# 使用携带 cookies 的会话发送 POST 请求
response = session.post(
    'https://lowelab.ucsc.edu/cgi-bin/tRNAscan-SE2.cgi',
    data=payload,
    files=files
)

# 打印响应的 HTML 内容
print(response.text)
