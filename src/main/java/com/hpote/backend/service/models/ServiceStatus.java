package com.hpote.backend.service.models;

import com.hpote.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceStatus extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name; // PENDING, ACCEPTED, COMPLETED

    private String description;
}
