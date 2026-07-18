FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY transformer/enrich/plet-geo-enrich/main.py transformer/enrich/plet-geo-enrich/logic.py transformer/enrich/plet-geo-enrich/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-geo-enrich
ENTRYPOINT ["python", "/app/main.py"]
