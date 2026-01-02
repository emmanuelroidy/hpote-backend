package com.hpote.backend.professional.models;

import com.hpote.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "professions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profession extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name; // Doctor, Mechanic, Seller

    private String description;

    private Boolean isActive = true;
}
