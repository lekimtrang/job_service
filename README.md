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


