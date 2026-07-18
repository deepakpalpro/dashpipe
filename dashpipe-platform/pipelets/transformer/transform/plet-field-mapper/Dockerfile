FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY transformer/transform/plet-field-mapper/main.py transformer/transform/plet-field-mapper/pipelet.json transformer/transform/plet-field-mapper/mapper.py /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-field-mapper
ENTRYPOINT ["python", "/app/main.py"]
