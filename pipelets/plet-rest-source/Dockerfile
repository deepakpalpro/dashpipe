FROM python:3.12-slim

WORKDIR /app

COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt

COPY _common /app/_common
COPY plet-rest-source/config.py plet-rest-source/rest_source.py plet-rest-source/main.py plet-rest-source/pipelet.json /app/

ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-rest-source

ENTRYPOINT ["python", "/app/main.py"]
