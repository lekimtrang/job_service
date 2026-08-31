# job_service
Job processing service

# Architecture Overview
[POST /api/jobs] -> Save PENDING to DB -> Enqueue to RabbitMQ
                                              |
                                              v
[RabbitMQ Listener] <- [Redis Lock Check] <- Worker Processes Job
                                              |
                     +------------------------+------------------------+

                     | (Success)                                       | ("fail": true)
                     v                                                 v
             Mark COMPLETED                                    Retry up to 3 times
                                                                       | (After 3 failures)
                                                                       v
                                                                Mark FAILED + Log Error
________________________________________

# Installation 
Docker Desktop

rabbitmq-server-4.3.5

postgresql-17.11-1

Erlang 29.0.5

docker run --name my-redis -p 6379:6379 -d redis

mvn clean spring-boot:run 

<img width="2211" height="1935" alt="image" src="https://github.com/user-attachments/assets/7e64b723-38df-4a5c-af89-1bb28b6e5446" />

<img width="3153" height="1386" alt="image" src="https://github.com/user-attachments/assets/1e4a3023-cf15-4256-8224-c6c237ae10d9" />

