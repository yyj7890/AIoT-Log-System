FROM python:3.11-slim
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app.py .
ENV PIPER_MODEL=/models/voice.onnx
EXPOSE 8090
USER 65532:65532
ENTRYPOINT ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8090"]
