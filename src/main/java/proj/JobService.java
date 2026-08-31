package proj;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

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
        Job job = new Job();
        job.setType(type);
        job.setPayload(payload);
        job.setStatus(JobStatus.PENDING);
        job.setCreatedAt(LocalDateTime.now());
        Job savedJob = jobRepository.save(job);
        
        rabbitTemplate.convertAndSend(RabbitMQConfig.JOB_QUEUE, savedJob.getId().toString());
        return savedJob.getId();
    }

    public void processJob(Long jobId) {
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

    @Transactional
    public void executeJobLogic(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("Job not found"));

        // Protect against processing jobs that are finished or already running
        if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.FAILED || job.getStatus() == JobStatus.PROCESSING) {
        	return;
        }

        job.setStatus(JobStatus.PROCESSING);
        job.setUpdateddAt(LocalDateTime.now());
        jobRepository.saveAndFlush(job);

        try {
            // Rule validation: if payload contains "fail": true, fail execution
            if (job.getPayload() != null && job.getPayload().contains("\"fail\": true")) {
                throw new RuntimeException("Simulated job payload error failure constraint triggered.");
            }

            // Success Execution Path
            job.setStatus(JobStatus.COMPLETED);
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
            job.setUpdateddAt(LocalDateTime.now());
            jobRepository.save(job);
        } else {
            job.setStatus(JobStatus.PENDING); // Mark back to pending so it can be picked up safely
            job.setUpdateddAt(LocalDateTime.now());
            jobRepository.save(job);
            
            // Re-queue the job ID for asynchronous retry loop mitigation
            rabbitTemplate.convertAndSend(RabbitMQConfig.JOB_QUEUE, job.getId().toString());
        }
    }
}