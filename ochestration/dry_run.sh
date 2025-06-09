#!/bin/bash

mvn clean package

nohup ./target/server --name 1 --configPath config/config.json > logs/replica_1.log &
nohup ./target/server --name 2 --configPath config/config.json > logs/replica_2.log &
nohup ./target/server --name 3 --configPath config/config.json > logs/replica_3.log &
nohup ./target/server --name 4 --configPath config/config.json > logs/replica_4.log &
nohup ./target/server --name 5 --configPath config/config.json > logs/replica_5.log &

echo "All 5 replicas started."

sleep 10

./target/client --name 10 --configPath config/config.json --duration 60 > logs/client_10.log &

echo "Client started."

for i in {1..5}; do
  pkill server
done

echo "All replicas stopped."