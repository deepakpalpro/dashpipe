FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY transformer/quality/plet-ml-scorer/main.py transformer/quality/plet-ml-scorer/logic.py transformer/quality/plet-ml-scorer/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-ml-scorer
ENTRYPOINT ["python", "/app/main.py"]
