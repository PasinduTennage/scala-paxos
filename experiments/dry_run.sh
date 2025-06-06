#!/bin/bash

NUM_REPLICAS=5


nohup ./target/server --name 1 --configPath config/config.json > logs/replica_1.log &
nohup ./target/server --name 2 --configPath config/config.json > logs/replica_2.log &
nohup ./target/server --name 3 --configPath config/config.json > logs/replica_3.log &
nohup ./target/server --name 4 --configPath config/config.json > logs/replica_4.log &
nohup ./target/server --name 5 --configPath config/config.json > logs/replica_5.log &


sleep 20

for i in $(seq 1 $NUM_REPLICAS); do
    pkill server
done

echo "All $NUM_REPLICAS replicas started."