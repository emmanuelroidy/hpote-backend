package com.hpote.backend.user.model;

import com.hpote.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name; // NORMAL, PROFESSIONAL, ADMIN

    private String description;
}
