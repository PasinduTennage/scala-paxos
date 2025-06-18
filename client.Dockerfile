# client.Dockerfile
FROM alpine:latest

WORKDIR /app

COPY target/client ./client

RUN chmod +x ./client

ENTRYPOINT ["./client"]
