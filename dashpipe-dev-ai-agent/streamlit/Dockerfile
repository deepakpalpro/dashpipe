FROM python:3.12-slim
WORKDIR /app
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/api:/app/streamlit

COPY dashpipe-mcp /dashpipe-mcp
COPY api/requirements.txt /app/api/requirements.txt
COPY api/app /app/api/app
COPY streamlit/requirements.txt /app/streamlit/requirements.txt
COPY streamlit /app/streamlit

RUN pip install --no-cache-dir -e /dashpipe-mcp \
    && pip install --no-cache-dir -r /app/api/requirements.txt \
    && pip install --no-cache-dir -r /app/streamlit/requirements.txt

WORKDIR /app/streamlit
EXPOSE 8501
CMD ["streamlit", "run", "main.py", "--server.port=8501", "--server.address=0.0.0.0"]
