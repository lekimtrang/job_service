package proj;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {
	Page<Job> findByStatus(JobStatus status, Pageable pageable);
    List<Job> findByStatus(JobStatus status);
}