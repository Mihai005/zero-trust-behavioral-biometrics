package com.zerotrust.dto;

import lombok.Data;

@Data
public class KeystrokeDTO {
    private String key;
    private long timestamp;
    private int duration;
    private String type;
}
