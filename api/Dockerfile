FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

ENV PORT=8000
EXPOSE 8000

# Multiple workers = multiple processes handling requests within one
# container; run several containers behind a load balancer (all pointed at
# the same REDIS_URL) for real horizontal scaling.
CMD ["sh", "-c", "uvicorn main:app --host 0.0.0.0 --port ${PORT} --workers 2"]
