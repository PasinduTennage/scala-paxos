# server.Dockerfile
FROM alpine:latest

WORKDIR /app

COPY target/server ./server

RUN chmod +x ./server

ENTRYPOINT ["./server"]
