package com.hpote.backend.user.model;

import java.util.HashSet;
import java.util.Set;

import com.hpote.backend.common.BaseEntity;
import com.hpote.backend.professional.models.Profession;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(unique = true)
    private String email;

    private String password;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private UserRole role;

    private String profilePhoto;

    private Double rating = 0.0;



    @ManyToMany
    @JoinTable(
        name = "user_professions",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "profession_id")
    )
    private Set<Profession> professions = new HashSet<>();
}
