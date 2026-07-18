FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY common.py main.py ./

ENV PYTHONUNBUFFERED=1
ENV RUN_MODE=batch

ENTRYPOINT ["python", "/app/main.py"]
