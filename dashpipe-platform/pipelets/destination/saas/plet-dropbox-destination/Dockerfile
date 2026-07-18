FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY destination/saas/plet-dropbox-destination/main.py destination/saas/plet-dropbox-destination/logic.py destination/saas/plet-dropbox-destination/pipelet.json /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-dropbox-destination
ENTRYPOINT ["python", "/app/main.py"]
