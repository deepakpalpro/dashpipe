FROM python:3.12-slim

WORKDIR /app

COPY plet-csv-source/requirements.txt /tmp/requirements-csv-source.txt
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-csv-source.txt -r /tmp/requirements-amqp.txt

COPY _common /app/_common
COPY plet-csv-source/config.py plet-csv-source/csv_source.py plet-csv-source/main.py plet-csv-source/pipelet.json /app/

ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-csv-source

ENTRYPOINT ["python", "/app/main.py"]
