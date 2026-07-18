FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY transformer/structure/plet-aggregator/main.py transformer/structure/plet-aggregator/logic.py transformer/structure/plet-aggregator/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-aggregator
ENTRYPOINT ["python", "/app/main.py"]
