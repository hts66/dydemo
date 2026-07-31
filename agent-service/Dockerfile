FROM python:3.12-slim

WORKDIR /app

# 安装依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt \
    -i https://pypi.tuna.tsinghua.edu.cn/simple

# 复制源码
COPY . .

EXPOSE 8000

# 生产模式启动
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
