# Email Campaign API — DevOps Practice Project

A small production-style Spring Boot API used as a hands-on DevOps laboratory.

## Stack

- Java 21 / Spring Boot
- PostgreSQL + Flyway
- Apache Kafka (KRaft)
- Spring Security + JWT
- Docker / Docker Compose
- GitHub Actions
- Spring Boot Actuator

## Architecture

```text
Client
  |
  v
Spring Boot API :8080
  |             |
  v             v
PostgreSQL    Kafka :9094
                |
                v
        Campaign Ops Listener
```

When a campaign is processed, the API publishes a `campaign-events` Kafka event. The built-in ops listener consumes the event and logs it. This gives the project a real message-broker workflow without requiring a second application service.

## Deployment model

The application is designed to run directly with Docker Compose on an EC2 host. PostgreSQL data and Kafka data use named Docker volumes. Application configuration is supplied through environment variables; secrets should be provided through the deployment host's `.env` file or CI/CD secret mechanism and never committed to Git.

## Run the stack

```bash
cp .env.example .env
# edit .env and set real secrets
docker compose up -d --build
docker compose ps
```

Health check:

```text
http://<host>:8080/actuator/health
```

Swagger:

```text
http://<host>:8080/swagger-ui.html
```

## Kafka

Inside the Docker network, the application connects to `kafka:9094`. Port `9092` is the external listener for host-side Kafka testing. The topic `campaign-events` is created automatically by the application.

Useful checks:

```bash
docker compose logs -f app
docker compose logs -f kafka
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9094 --list
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9094 --topic campaign-events --from-beginning
```

## DevOps learning path

1. Git/GitHub
2. Docker image and container lifecycle
3. Docker Compose
4. AWS EC2 deployment
5. Nginx reverse proxy
6. Environment variables and secrets
7. GitHub Actions CI
8. Automated CD to EC2
9. CloudWatch metrics and logs
10. Alerting with SNS
11. Terraform infrastructure
12. Reliability, scaling and rollback
13. Kubernetes

The goal is to demonstrate the operational actions and outcomes rather than the complexity of the business application.
