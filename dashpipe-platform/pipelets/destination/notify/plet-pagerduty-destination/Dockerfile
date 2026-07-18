FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY destination/notify/plet-pagerduty-destination/main.py destination/notify/plet-pagerduty-destination/logic.py destination/notify/plet-pagerduty-destination/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-pagerduty-destination
ENTRYPOINT ["python", "/app/main.py"]
