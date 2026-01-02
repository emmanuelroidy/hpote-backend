package com.hpote.backend.professional.models;

import com.hpote.backend.common.BaseEntity;
import com.hpote.backend.user.model.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "professional_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfessionalProfile extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne
    @JoinColumn(name = "profession_id")
    private Profession profession;

    private Double latitude;
    private Double longitude;

    private Double serviceRadiusKm;

    private Boolean isAvailable = true;
    private Boolean isVerified = false;
}
