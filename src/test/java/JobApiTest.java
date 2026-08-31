import com.fasterxml.jackson.databind.ObjectMapper;

import proj.Job;
import proj.JobApplication;
import proj.JobRepository;
import proj.JobRequest;
import proj.JobStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = JobApplication.class)
@AutoConfigureMockMvc
public class JobApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
    }
    
    @Test
    @Transactional
    void testCreateJob_ShouldReturnId() throws Exception {
        JobRequest request = new JobRequest();
        request.setType("DATA_EXPORT");
        request.setPayload("{\"fail\": false}");

        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    
    @Test
    @Transactional
    void testGetJobById_WhenExists_ShouldReturnDetails() throws Exception {
        Job job = jobRepository.save(Job.builder().type("TEST").status(JobStatus.PENDING).build());

        mockMvc.perform(get("/api/jobs/" + job.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(job.getId().intValue())))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }
    
    @Test
    @Transactional
    void testGetJobById_WhenNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/jobs/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void testListJobsWithFilteringAndPagination() throws Exception {
        jobRepository.save(Job.builder().type("T1").status(JobStatus.PENDING).build());
        jobRepository.save(Job.builder().type("T2").status(JobStatus.COMPLETED).build());

        mockMvc.perform(get("/api/jobs?status=PENDING&page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is("PENDING")));
    }
    
}
