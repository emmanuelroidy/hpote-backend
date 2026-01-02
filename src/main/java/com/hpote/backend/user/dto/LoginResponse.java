package com.hpote.backend.user.dto;
import lombok.Data;
import lombok.Builder;
import java.util.UUID;



@Data
@Builder
public class LoginResponse {
    private String token;
    private UUID userId;
    private String name;
    private String role;
}

