package com.hpote.backend.service.models;

import com.hpote.backend.common.BaseEntity;
import com.hpote.backend.professional.models.Profession;
import com.hpote.backend.user.model.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequest extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne
    @JoinColumn(name = "profession_id")
    private Profession profession;

    @Column(length = 1000)
    private String description;
    private String pictureUrl;

    private Double latitude;
    private Double longitude;

    @ManyToOne
    @JoinColumn(name = "status_id")
    private ServiceStatus status;
}
