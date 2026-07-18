FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY source/http/plet-rest-source/main.py source/http/plet-rest-source/config.py source/http/plet-rest-source/pipelet.json source/http/plet-rest-source/rest_source.py /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-rest-source
ENTRYPOINT ["python", "/app/main.py"]
