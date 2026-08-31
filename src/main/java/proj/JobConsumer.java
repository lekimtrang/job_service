package proj;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobConsumer {
	@Autowired
    private JobService jobService;

    @RabbitListener(queues = RabbitMQConfig.JOB_QUEUE)
    public void receiveMessage(String jobId) {
        jobService.processJob(Long.parseLong(jobId));
    }
}