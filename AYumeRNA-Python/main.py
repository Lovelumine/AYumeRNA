from flask import Flask, request, jsonify
import os
from scripts.make_onehot_from_traceback import make_onehot_of_cm_from_traceback

app = Flask(__name__)

@app.route('/process_traceback', methods=['POST'])
def process_traceback():
    data = request.get_json()
    traceback_file = data.get('traceback')
    cmfile = data.get('cmfile')

    if not traceback_file or not cmfile:
        return jsonify({"error": "Missing traceback or cmfile"}), 400

    try:
        # 调用 make_onehot_from_traceback.py 中的函数
        output_h5 = make_onehot_of_cm_from_traceback(traceback_file, cmfile)
        return jsonify({"output_file": output_h5})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=2002)
