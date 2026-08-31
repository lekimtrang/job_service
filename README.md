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

Run redis via docker: docker run --name my-redis -p 6379:6379 -d redis

Ensure a PostgreSQL instance is running with a database named 'postgres'

Ensure RabbitMQ Server has been already started

Run the application using your IDE or via terminal: mvn clean spring-boot:run 

Use an API client (like Postman or cURL) to verify the requests

http://localhost:9090

<img width="2922" height="1461" alt="image" src="https://github.com/user-attachments/assets/52f259d7-e61f-4ea5-8878-13f2d9dc0a1d" />

# 📁 Project Structure
src/main/java/proj/

├── JobApplication.java

├── config

│   ├── RabbitMQConfig.java

│   └── RedisConfig.java

├── controller

│   └── JobController.java

├── dto

│   └── JobRequest.java

├── exception

│   └── GlobalExceptionHandler.java

├── model

│   ├── Job.java

│   └── JobStatus.java

├── repository

│   └── JobRepository.java

└── service

    └── JobService.java
    


# Postman test

## 1.	POST /api/jobs	- Create a new job	- Create a job with status PENDING and return the created job ID.

<img width="1105" height="967" alt="image" src="https://github.com/user-attachments/assets/7e64b723-38df-4a5c-af89-1bb28b6e5446" />


<img width="1576" height="693" alt="image" src="https://github.com/user-attachments/assets/1e4a3023-cf15-4256-8224-c6c237ae10d9" />


<img width="1095" height="726" alt="image" src="https://github.com/user-attachments/assets/313a8e00-97a0-45fa-852d-218d20ef6dc3" />

## 2. GET /api/jobs/{id} - Get job details - Return the job details. Return 404 or a proper error response if the job does not exist.

<img width="2202" height="1656" alt="image" src="https://github.com/user-attachments/assets/33fdc7b3-18d7-412e-ab28-517a9a01ccdd" />


<img width="2208" height="1503" alt="image" src="https://github.com/user-attachments/assets/4fbdeb2c-ed39-4b6e-b5fc-f33c38d274a7" />

## 3. GET /api/jobs?status=PENDING&page=0&size=20 - List jobs - Support filtering by status and pagination.

<img width="2196" height="1842" alt="image" src="https://github.com/user-attachments/assets/3f8da8be-9a83-4516-ad22-ebf65ddd7cf7" />


<img width="2232" height="1869" alt="image" src="https://github.com/user-attachments/assets/6a811e49-74ce-479a-9174-0fa8dea98ace" />

## 4. POST /api/jobs/process - Process pending jobs - Pick pending jobs, process them, update status, retry failures, and protect against duplicate concurrent processing.

<img width="2184" height="1362" alt="image" src="https://github.com/user-attachments/assets/afdd6d60-68bf-49e6-b8a1-c724862cac14" />


## 5 Concurrency

<img width="3402" height="270" alt="image" src="https://github.com/user-attachments/assets/4026f12f-0d65-47fa-979c-aa483797cb39" />

### PENDING->PROCESSING->PENDING->PROCESSING->PENDING->PROCESSING->FAILED

<img width="2775" height="1632" alt="image" src="https://github.com/user-attachments/assets/ab740fcb-e6cc-46a2-b9b2-e145a2da2a59" />

### QUESTION A: Scale-Up Strategy (1 Million Jobs/Day)

Message Broker (Redis/RabbitMQ/Kafka): Acts as the high-throughput, distributed runtime queue to coordinate worker instances.

Worker Instances: Dedicated background instances that consume jobs from the broker, execute the logic, and update the database status.

[ Clients ] ---> [ Spring Boot API Layer ]
                         |
                 (1. Write State)
                         v
                 [ PostgreSQL DB ] 
                         |
              (2. Stream / Publish)
                         v
               [ Redis / RabbitMQ ] <--- (3. Pull / Process) --- [ Spring Boot Workers ]


### QUESTION B: Query Optimization for 50M Records

% Issue: 

Missing Index: The database executes an aggressive Sequential Scan over millions of dead historical variables just to filter out sparse PENDING states.

The High Offset Trap: When requesting standard offsets deep into an execution index, Postgres must sequentially load every record up to that point just to discard them (e.g., parsing pages size=20, page=10000).

% Investigation:

Run a comprehensive database analysis check using EXPLAIN ANALYZE:

EXPLAIN ANALYZE SELECT * FROM jobs WHERE status = 'PENDING' ORDER BY created_at DESC LIMIT 20 OFFSET 0;

% Solution:

Partial Functional Indexes: Because active states like PENDING and PROCESSING consume only a tiny sliver of a 50M table compared to COMPLETED, build a high-performance Partial Index:

CREATE INDEX idx_jobs_pending_partial ON jobs (created_at DESC) 
WHERE status IN ('PENDING', 'PROCESSING');

Modify query string

SELECT * FROM jobs 
WHERE status = 'PENDING' AND id < :last_seen_id 
ORDER BY id DESC LIMIT 20;




