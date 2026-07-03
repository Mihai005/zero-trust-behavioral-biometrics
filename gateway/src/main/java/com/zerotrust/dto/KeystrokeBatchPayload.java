package com.zerotrust.dto;

import lombok.Data;

import java.util.List;

@Data
public class KeystrokeBatchPayload {
    private String userId;
    private List<KeystrokeDTO> keystrokes;
}
