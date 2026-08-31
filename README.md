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

http://localhost:9090

<img width="2922" height="1461" alt="image" src="https://github.com/user-attachments/assets/52f259d7-e61f-4ea5-8878-13f2d9dc0a1d" />


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




## 4. 
