from flask import Flask
from routes.traceback import traceback_bp
from routes.split_onehot import split_onehot_bp

app = Flask(__name__)

# 注册蓝图
app.register_blueprint(traceback_bp)
app.register_blueprint(split_onehot_bp)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=2002)
