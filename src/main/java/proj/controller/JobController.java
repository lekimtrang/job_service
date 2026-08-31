package proj.controller;


import lombok.RequiredArgsConstructor;
import proj.dto.JobRequest;
import proj.model.Job;
import proj.model.JobStatus;
import proj.service.JobService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

	@Autowired
    private JobService jobService;

    @PostMapping
    public ResponseEntity<Map<String, Long>> createJob(@RequestBody JobRequest request) throws JsonProcessingException{// Map<String, Object> rawPayload) {
    	//String type = (String)rawPayload.get("type");
    	ObjectMapper mapper = new ObjectMapper();
		//try {
			//String payload = (String)mapper.writeValueAsString(rawPayload.get("payload"));
			//Long id = jobService.createJob(type, payload);
    		String payload = (String)mapper.writeValueAsString(request.getPayload());
			Long id = jobService.createJob(request.getType(), payload);
			return ResponseEntity.ok(Map.of("id", id));
		//} catch (JsonProcessingException e) {
			//e.printStackTrace();
		//}
		//return null;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }
    

    @GetMapping
    public ResponseEntity<Page<Job>> getJobs(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(jobService.getJobs(status, PageRequest.of(page, size)));
    }


    
    // preventing concurrent 
    @PostMapping("/processId")
    public ResponseEntity<String> processJob(@RequestParam Long jobId) {
        jobService.processPendingJob(jobId);
        return ResponseEntity.ok("Processing jobId " + jobId +" completed or skipped via mutual exclusion.");
    }
    
    // preventing concurrent duplication
    @PostMapping("/process")   
    public ResponseEntity<String> processJobsConcurrently() {
        jobService.processJobs();
        return ResponseEntity.ok("Processing sequence completed or skipped via mutual exclusion.");
    }
}