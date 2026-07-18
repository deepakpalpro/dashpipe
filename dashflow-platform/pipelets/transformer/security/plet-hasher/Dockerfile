FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY transformer/security/plet-hasher/main.py transformer/security/plet-hasher/logic.py transformer/security/plet-hasher/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-hasher
ENTRYPOINT ["python", "/app/main.py"]
