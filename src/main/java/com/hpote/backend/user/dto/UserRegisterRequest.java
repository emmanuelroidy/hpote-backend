package com.hpote.backend.user.dto;

import com.hpote.backend.user.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Set;
import java.util.UUID;

@Data
public class UserRegisterRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Size(max = 25)
    private String phone;

    @Email
    @Size(max = 254)
    private String email;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

  
    @NotBlank
     private String role; // ✅ STRING



      private Set<UUID> professionIds;     // REQUIRED if PROFESSIONAL

}
