FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY source/messaging/plet-amqp-source/main.py source/messaging/plet-amqp-source/logic.py source/messaging/plet-amqp-source/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-amqp-source
ENTRYPOINT ["python", "/app/main.py"]
