FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY transformer/structure/plet-splitter/main.py transformer/structure/plet-splitter/logic.py transformer/structure/plet-splitter/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-splitter
ENTRYPOINT ["python", "/app/main.py"]
