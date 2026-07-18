FROM python:3.12-slim
WORKDIR /app
COPY _common/requirements-amqp.txt /tmp/requirements-amqp.txt
RUN pip install --no-cache-dir -r /tmp/requirements-amqp.txt
COPY _common /app/_common
COPY transformer/filter/plet-python-filter/main.py transformer/filter/plet-python-filter/pipelet.json transformer/filter/plet-python-filter/filter_expr.py /app/
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/_common
ENV PIPELET_ID=plet-python-filter
ENTRYPOINT ["python", "/app/main.py"]
