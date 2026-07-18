FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY transformer/transform/plet-type-coercer/main.py transformer/transform/plet-type-coercer/logic.py transformer/transform/plet-type-coercer/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-type-coercer
ENTRYPOINT ["python", "/app/main.py"]
