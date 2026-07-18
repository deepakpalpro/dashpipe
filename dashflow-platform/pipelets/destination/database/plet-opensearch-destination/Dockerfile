FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
COPY destination/database/plet-opensearch-destination/requirements.txt /tmp/requirements-extra.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt -r /tmp/requirements-extra.txt
COPY _common /app/_common
COPY destination/database/plet-opensearch-destination/main.py destination/database/plet-opensearch-destination/logic.py destination/database/plet-opensearch-destination/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-opensearch-destination
ENTRYPOINT ["python", "/app/main.py"]
