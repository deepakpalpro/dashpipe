FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY source/http/plet-http-poll-source/main.py source/http/plet-http-poll-source/logic.py source/http/plet-http-poll-source/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-http-poll-source
ENTRYPOINT ["python", "/app/main.py"]
