package proj;


import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
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
    public ResponseEntity<Map<String, Long>> createJob(@RequestBody Map<String, Object> rawPayload) {
    	String type = (String)rawPayload.get("type");
    	ObjectMapper mapper = new ObjectMapper();
    	String payload;
		try {
			payload = (String)mapper.writeValueAsString(rawPayload.get("payload"));
			Long id = jobService.createJob(type, payload);
			return ResponseEntity.ok(Map.of("id", id));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }
    




    
    // preventing concurrent duplication
    @PostMapping("/process")
    public ResponseEntity<String> processJobConcurrently(@RequestParam Long jobId) {
        jobService.processJob(jobId);
        return ResponseEntity.ok("Processing sequence completed or skipped via mutual exclusion.");
    }
}