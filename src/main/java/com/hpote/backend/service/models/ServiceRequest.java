package com.hpote.backend.service.models;

import com.hpote.backend.common.BaseEntity;
import com.hpote.backend.professional.models.Profession;
import com.hpote.backend.user.model.User;

import java.util.ArrayList;
import java.util.List;

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

    @Column(length = 2048)
    private String pictureUrl;

    @ElementCollection
    @CollectionTable(name = "service_request_photos", joinColumns = @JoinColumn(name = "service_request_id"))
    @Column(name = "picture_url", nullable = false, length = 2048)
    @OrderColumn(name = "display_order")
    @Builder.Default
    private List<String> additionalPictureUrls = new ArrayList<>();

    private Double latitude;
    private Double longitude;

    @ManyToOne
    @JoinColumn(name = "status_id")
    private ServiceStatus status;

    @Version
    private Long version;
}
