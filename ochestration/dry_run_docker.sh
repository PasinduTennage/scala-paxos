#!/bin/bash

# Stop and remove all replica containers
docker stop replica_1 replica_2 replica_3 replica_4 replica_5 client_10
docker rm replica_1 replica_2 replica_3 replica_4 replica_5 client_10

docker kill $(docker ps -q)

rm -rf logs
mkdir logs
echo "deleted and created logs/..."

mvn clean package

docker build -t scala-client -f client.Dockerfile .
docker build -t scala-server -f server.Dockerfile .

docker run -d --name replica_1 --network host -v $(pwd)/config:/config -v $(pwd)/logs:/logs -p 10001:10001 -p 11001:11001 scala-server --name 1 --configPath /config/config.json
docker run -d --name replica_2 --network host -v $(pwd)/config:/config -v $(pwd)/logs:/logs -p 10002:10002 -p 11002:11002 scala-server --name 2 --configPath /config/config.json
docker run -d --name replica_3 --network host -v $(pwd)/config:/config -v $(pwd)/logs:/logs -p 10003:10003 -p 11003:11003 scala-server --name 3 --configPath /config/config.json
docker run -d --name replica_4 --network host -v $(pwd)/config:/config -v $(pwd)/logs:/logs -p 10004:10004 -p 11004:11004 scala-server --name 4 --configPath /config/config.json
docker run -d --name replica_5 --network host -v $(pwd)/config:/config -v $(pwd)/logs:/logs -p 10005:10005 -p 11005:11005 scala-server --name 5 --configPath /config/config.json

echo "All 5 replicas started."

sleep 10

docker run --name client_10 --network host --rm -v $(pwd)/config:/config -v $(pwd)/logs:/logs scala-client --name 10 --configPath /config/config.json

echo "Client finished."

# Stop and remove all replica containers
docker stop replica_1 replica_2 replica_3 replica_4 replica_5
docker rm replica_1 replica_2 replica_3 replica_4 replica_5

echo "All replicas stopped."
