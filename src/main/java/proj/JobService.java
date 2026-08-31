package proj;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.InvalidParameterException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

	@Autowired
    private JobRepository jobRepository;
	@Autowired
    private RabbitTemplate rabbitTemplate;
	@Autowired
    private StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "lock:job:";
    private static final int MAX_RETRIES = 3;

    @Transactional
    public Long createJob(String type, String payload) {
        Job job = Job.builder()
        		.type(type)
                .payload(payload)
                .status(JobStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .statusFlow(JobStatus.PENDING.toString())
                .build();
        Job savedJob = jobRepository.save(job);
        
        rabbitTemplate.convertAndSend(RabbitMQConfig.JOB_QUEUE, savedJob.getId().toString());
        return savedJob.getId();
    }

    public void processPendingJob(Long jobId) {
        String lockKey = LOCK_PREFIX + jobId;
        // Attempt to acquire distributed lock (Expires in 5 minutes to prevent deadlocks)
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", Duration.ofMinutes(5));

        if (Boolean.FALSE.equals(acquired)) {
            // Already being processed elsewhere concurrently. Abort.
            return;
        }

        try {
            executeJobLogic(jobId);
        } finally {
            redisTemplate.delete(lockKey); // Release lock
        }
    }

    public void processJobs() {
    	List<Job> pendingList = jobRepository.findByStatus(JobStatus.PENDING);

        for(int i = 0 ; i < pendingList.size(); i++) {
        	processPendingJob(pendingList.get(i).getId());
        }
    }
    
    public void executeJobLogic(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("Job not found"));
        // Protect against processing jobs that are finished or already running
        if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.FAILED || job.getStatus() == JobStatus.PROCESSING) {
        	return;
        }
        job.setStatus(JobStatus.PROCESSING);
        job.setStatusFlow(job.getStatusFlow() + "->" + JobStatus.PROCESSING.toString());
        job.setUpdateddAt(LocalDateTime.now());
        jobRepository.saveAndFlush(job);
        try {
            // Rule validation: if payload contains "fail": true, fail execution
            if (job.getPayload() != null && job.getPayload().trim().contains("\"fail\":true")) {
                throw new RuntimeException("Simulated job payload error failure constraint triggered.");
            }

            // Success Execution Path
            job.setStatus(JobStatus.COMPLETED);
            job.setStatusFlow(job.getStatusFlow() + "->" + JobStatus.COMPLETED.toString());
            job.setUpdateddAt(LocalDateTime.now());
            job.setErrorMessage(null);
            jobRepository.save(job);

        } catch (Exception e) {
            handleJobFailure(job, e.getMessage());
        }
    }
    
    
    private void handleJobFailure(Job job, String errorMsg) {
        int currentRetries = job.getRetryCount() + 1;
        job.setRetryCount(currentRetries);
        job.setErrorMessage(errorMsg);

        if (currentRetries >= MAX_RETRIES) {
            job.setStatus(JobStatus.FAILED);
            job.setStatusFlow(job.getStatusFlow() + "->" + JobStatus.FAILED.toString());
            job.setUpdateddAt(LocalDateTime.now());
            jobRepository.save(job);
        } else {
            job.setStatus(JobStatus.PENDING); // Mark back to pending so it can be picked up safely
            job.setStatusFlow(job.getStatusFlow() + "->" + JobStatus.PENDING.toString());
            job.setUpdateddAt(LocalDateTime.now());
            jobRepository.save(job);
            
            // Re-queue the job ID for asynchronous retry loop mitigation
            rabbitTemplate.convertAndSend(RabbitMQConfig.JOB_QUEUE, job.getId().toString());
        }
    }
  
    public Page<Job> getJobs(JobStatus status, Pageable pageable) {
        if (status != null) {
        	Page<Job> jobPage =  jobRepository.findByStatus(status, pageable);
            if (jobPage.isEmpty()) {
                throw new InvalidParameterException("Job status " + status.toString() + " not found in the database.");
            }
            return jobPage;
        }
        
        return jobRepository.findAll(pageable);
    }


    
    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found with id: " + id));
    }

}