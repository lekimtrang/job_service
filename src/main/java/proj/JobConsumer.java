package proj;

import lombok.RequiredArgsConstructor;
import proj.config.RabbitMQConfig;
import proj.model.Job;
import proj.repository.JobRepository;
import proj.service.JobService;

import java.util.Optional;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class JobConsumer {
	@Autowired
    private JobService jobService;
	
	@Autowired
    private JobRepository jobRepository;

    @RabbitListener(queues = RabbitMQConfig.JOB_QUEUE)
    public void receiveMessage(String jobId) {
    	Optional<Job> jobOpt = jobRepository.findById(Long.parseLong(jobId));
    	
        if (jobOpt.isEmpty()) {
            // Log it clearly and return safely so the message gets ACKed and removed from the queue
            return; 
        }
        jobService.processPendingJob(Long.parseLong(jobId));
    }
}