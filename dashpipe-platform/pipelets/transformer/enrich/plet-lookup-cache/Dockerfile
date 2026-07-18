FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY transformer/enrich/plet-lookup-cache/main.py transformer/enrich/plet-lookup-cache/logic.py transformer/enrich/plet-lookup-cache/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-lookup-cache
ENTRYPOINT ["python", "/app/main.py"]
