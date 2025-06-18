# client.Dockerfile
FROM ubuntu:22.04

WORKDIR /app

# Install basic dependencies (optional, e.g., libc, net-tools, etc.)
RUN apt-get update && apt-get install -y \
    ca-certificates \
    libstdc++6 \
    libgcc-s1 \
    && rm -rf /var/lib/apt/lists/*

COPY target/client ./client

RUN chmod +x ./client

ENTRYPOINT ["./client"]
