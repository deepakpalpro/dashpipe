FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY transformer/filter/plet-sample-filter/main.py transformer/filter/plet-sample-filter/logic.py transformer/filter/plet-sample-filter/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-sample-filter
ENTRYPOINT ["python", "/app/main.py"]
