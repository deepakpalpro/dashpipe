FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY transformer/transform/plet-avro-codec/main.py transformer/transform/plet-avro-codec/logic.py transformer/transform/plet-avro-codec/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-avro-codec
ENTRYPOINT ["python", "/app/main.py"]
