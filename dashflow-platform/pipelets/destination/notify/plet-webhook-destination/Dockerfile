FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY destination/notify/plet-webhook-destination/main.py destination/notify/plet-webhook-destination/pipelet.json destination/notify/plet-webhook-destination/webhook.py /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-webhook-destination
ENTRYPOINT ["python", "/app/main.py"]
