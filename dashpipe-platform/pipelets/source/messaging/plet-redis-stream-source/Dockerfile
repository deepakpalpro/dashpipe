FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY source/messaging/plet-redis-stream-source/main.py source/messaging/plet-redis-stream-source/logic.py source/messaging/plet-redis-stream-source/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-redis-stream-source
ENTRYPOINT ["python", "/app/main.py"]
