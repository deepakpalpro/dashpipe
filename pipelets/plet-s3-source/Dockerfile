FROM python:3.12-slim

WORKDIR /app

COPY plet-s3-source/requirements.txt /tmp/requirements-s3.txt
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-s3.txt -r /tmp/requirements-amqp.txt

COPY _common /app/_common
COPY plet-s3-source/config.py plet-s3-source/s3_source.py plet-s3-source/main.py plet-s3-source/pipelet.json /app/

ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-s3-source

ENTRYPOINT ["python", "/app/main.py"]
