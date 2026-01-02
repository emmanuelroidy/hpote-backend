package com.hpote.backend.user.dto;

import com.hpote.backend.user.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Set;
import java.util.UUID;

@Data
public class UserRegisterRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String phone;

    private String email;

    @NotBlank
    private String password;

  
    @NotBlank
     private String role; // ✅ STRING



      private Set<UUID> professionIds;     // REQUIRED if PROFESSIONAL

}
