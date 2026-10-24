# Distributed API Rate Limiter



A distributed API rate limiter built with Java, Spring Boot, Redis, Docker Compose, Prometheus, and Grafana. It uses an atomic Redis Lua script to enforce rate limits across multiple application instances.



## Features



- Redis-backed distributed rate limiting.

- Atomic request counting using a Redis Lua script.

- Configurable request limits and time windows.

- API-key-based rate limiting using the `X-API-KEY` header.

- HTTP `429 Too Many Requests` when the limit is exceeded.

- HTTP `503 Service Unavailable` when Redis is unavailable.

- Rate-limit response headers, including `X-RateLimit-Limit`, `X-RateLimit-Remaining`, and `X-RateLimit-Reset`.

- Two application instances sharing the same Redis backend.

- Prometheus metrics and Grafana monitoring.

- JUnit tests covering rate limiting and concurrent requests.



## Technology Stack



- Java 21

- Spring Boot

- Redis and Lua scripting

- Maven

- Docker Compose

- Prometheus

- Grafana

- JUnit 5



## Prerequisites



- Java 21

- Maven 3.9 or later, or use the included Maven Wrapper

- Docker Desktop with Docker Compose



## Run Locally



Run the test suite:



```bash

mvn clean test

```



Build the executable JAR:



```bash

mvn package

```



## Run with Docker Compose



Build and start the application instances, Redis, Prometheus, and Grafana:



```bash

docker compose up -d --build

```



Check the running containers:



```bash

docker compose ps

```



Stop the services:



```bash

docker compose down

```



## Endpoints



| Service | URL |

|---|---|

| Rate limiter instance 1 | http://localhost:8080 |

| Rate limiter instance 2 | http://localhost:8081 |

| Prometheus | http://localhost:9090 |

| Grafana | http://localhost:3000 |



The rate limiter test endpoint is `/api/test`.



## Test the Rate Limiter



Send requests with the same API key to both instances:



```bash

curl -i -H "X-API-KEY: demo-client" http://localhost:8080/api/test

curl -i -H "X-API-KEY: demo-client" http://localhost:8081/api/test

```



The default configuration used during testing allows three requests per 60-second window. After the limit is reached, further requests return HTTP `429`.



## Test Redis Failure Handling



Stop Redis:



```bash

docker compose stop redis

```



Send a request:



```bash

curl -i -H "X-API-KEY: outage-test" http://localhost:8080/api/test

```



When Redis is unavailable, the application returns HTTP `503 Service Unavailable`.



Restore Redis:



```bash

docker compose start redis

```



Requests should succeed again after Redis becomes available.



## Monitoring



Prometheus is available at `http://localhost:9090`, and Grafana is available at `http://localhost:3000`.



The Compose setup starts both monitoring services. Configure Grafana dashboards and the Prometheus data source as needed for your environment.



## Architecture



```text

Client

&#x20; |

&#x20; v

Rate Limiter Instance 1 ----+

&#x20;                           |

&#x20;                           v

&#x20;                        Redis

&#x20;                           ^

&#x20;                           |

Rate Limiter Instance 2 ----+



Prometheus collects metrics from the application instances.

Grafana visualizes the metrics.

```



## Design Highlights



The rate limiter uses Redis as shared state, allowing multiple application instances to enforce a common request limit. A Lua script performs the rate-limit operation atomically in Redis, helping prevent concurrent requests from bypassing the configured limit.



Redis failures are handled explicitly so the application returns a service-unavailable response instead of waiting indefinitely for the dependency.



## License



Add a license if you intend to distribute this project under a specific open-source license.




