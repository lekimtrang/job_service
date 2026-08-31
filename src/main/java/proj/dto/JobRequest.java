package proj.dto;

import lombok.Data;

@Data
public class JobRequest {
    private String type;
    private Object payload;
}
