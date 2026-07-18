FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY source/trigger/plet-schedule-source/main.py source/trigger/plet-schedule-source/logic.py source/trigger/plet-schedule-source/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-schedule-source
ENTRYPOINT ["python", "/app/main.py"]
