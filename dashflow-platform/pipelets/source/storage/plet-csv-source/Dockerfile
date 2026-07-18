FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
COPY source/storage/plet-csv-source/requirements.txt /tmp/requirements-extra.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt -r /tmp/requirements-extra.txt
COPY _common /app/_common
COPY source/storage/plet-csv-source/main.py source/storage/plet-csv-source/config.py source/storage/plet-csv-source/pipelet.json source/storage/plet-csv-source/csv_source.py /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-csv-source
ENTRYPOINT ["python", "/app/main.py"]
