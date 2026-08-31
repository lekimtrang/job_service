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
    public ResponseEntity<Job> createJob(@RequestBody Map<String, Object> rawPayload) {
    	String type = (String)rawPayload.get("type");
    	ObjectMapper mapper = new ObjectMapper();
    	String payload;
		try {
			payload = (String)mapper.writeValueAsString(rawPayload.get("payload"));
			return ResponseEntity.ok(jobService.createJob(type, payload));
		} catch (JsonProcessingException e) {
			
			e.printStackTrace();
		}
		return null;
        
    }
    
    // preventing concurrent duplication
    @PostMapping("/process")
    public ResponseEntity<String> processJobConcurrently(@RequestParam Long jobId) {
        jobService.processJob(jobId);
        return ResponseEntity.ok("Processing sequence completed or skipped via mutual exclusion.");
    }
}