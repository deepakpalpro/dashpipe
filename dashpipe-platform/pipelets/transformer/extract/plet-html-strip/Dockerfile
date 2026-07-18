FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY transformer/extract/plet-html-strip/main.py transformer/extract/plet-html-strip/logic.py transformer/extract/plet-html-strip/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-html-strip
ENTRYPOINT ["python", "/app/main.py"]
