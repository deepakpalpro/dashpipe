FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY destination/messaging/plet-sqs-destination/main.py destination/messaging/plet-sqs-destination/logic.py destination/messaging/plet-sqs-destination/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-sqs-destination
ENTRYPOINT ["python", "/app/main.py"]
