FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY source/storage/plet-file-watch-source/main.py source/storage/plet-file-watch-source/logic.py source/storage/plet-file-watch-source/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-file-watch-source
ENTRYPOINT ["python", "/app/main.py"]
