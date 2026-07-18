FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
COPY source/database/plet-bigquery-source/requirements.txt /tmp/requirements-extra.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt -r /tmp/requirements-extra.txt
COPY _common /app/_common
COPY source/database/plet-bigquery-source/main.py source/database/plet-bigquery-source/logic.py source/database/plet-bigquery-source/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-bigquery-source
ENTRYPOINT ["python", "/app/main.py"]
