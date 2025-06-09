# Paxos implementation in Scala


This project implements the `2-phase Paxos` protocol in `Scala`.

## Features

* `2-phase Paxos algorithm` where each proposer runs `prepare-promise` and `propose-accept` phases for each command.
* Client-side request batching.
* Replica-side request batching.

## Limitations and future work

* This code **is not production-ready** and **has not been tested** in real-world scenarios.
* Implementation of **leader-based Multi-Paxos** is left as future work.
* Implementation of **pipelining** is left as future work.

## How to run

* Refer to `orchestration/dry_run.sh` for instructions about building and running the codebase.

## Project structure

* `config/config.json` contains the IP and port of each replica.
* `src/main/scala/paxos/shared/Type.scala` contains the message definitions.
* `src/main/scala/paxos/server` contains the replica implementation.
* `src/main/scala/paxos/client` contains the client implementation.

## Bugs / Discussion / Feedback

* Please contact **pasindu.tennage@gmail.com** for any collaboration or pull requests.

## Use in production

* This repo **is not well-tested** and **lacks important implementation aspects**. It **should not be used** in life-critical or production environments.
* The **contributors (Pasindu Tennage)** takes **no responsibility** for any damage caused by using this software.