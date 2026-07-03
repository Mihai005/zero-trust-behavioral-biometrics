package com.zerotrust.dto;

import lombok.Data;

import java.util.Date;

@Data
public class DocumentResponseDTO {
    private Long id;
    private String content;
    private Date lastModified;
}
