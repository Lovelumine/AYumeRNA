from flask import Flask
from routes.traceback import traceback_bp
from routes.split_onehot import split_onehot_bp
from routes.generate_weight import generate_weight_bp
from routes.train import train_bp  # 导入新的训练蓝图

app = Flask(__name__)

# 注册蓝图
app.register_blueprint(generate_weight_bp)
app.register_blueprint(traceback_bp)
app.register_blueprint(split_onehot_bp)
app.register_blueprint(train_bp)  # 注册新的训练蓝图

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=2002)
