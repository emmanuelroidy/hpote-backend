package com.hpote.backend.common;

import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.hpote.backend.professional.models.Profession;
import com.hpote.backend.user.model.UserRole;
import com.hpote.backend.user.repository.UserRoleRepository;
import com.hpote.backend.professional.repository.ProfessionRepository;
import com.hpote.backend.service.models.ServiceStatus;
import com.hpote.backend.service.repository.ServiceStatusRepository;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRoleRepository userRoleRepository;
    private final ProfessionRepository professionRepository;
    private final ServiceStatusRepository serviceStatusRepository;

    @PostConstruct
    public void initUserRoles() {
        String[] roles = {"NORMAL", "PROFESSIONAL", "ADMIN"};

        for (String roleName : roles) {
            userRoleRepository.findByName(roleName)
                    .orElseGet(() -> userRoleRepository.save(
                            UserRole.builder().name(roleName).build()
                    ));
        }

        System.out.println("Default user roles initialized!");
    }


    @PostConstruct
    public void init() {
        String[] professions = {
            "DOCTOR",
            "MECHANIC",
            "ELECTRICIAN",
            "PLUMBER",
            "CARPENTER"
        };

        for (String name : professions) {
            professionRepository.findByName(name)
                .orElseGet(() -> professionRepository.save(
                    Profession.builder().name(name).build()
                ));
        }

        String[] serviceStatuses = {"OPEN", "ACCEPTED", "IN_PROGRESS", "COMPLETED", "CANCELLED"};
        for (String name : serviceStatuses) {
            serviceStatusRepository.findByName(name)
                    .orElseGet(() -> serviceStatusRepository.save(
                            ServiceStatus.builder().name(name).build()));
        }

    }

    @PostConstruct
    public void activateLegacyProfessions() {
        List<Profession> legacyProfessions = professionRepository.findByIsActiveIsNull();
        legacyProfessions.forEach(profession -> profession.setIsActive(true));
        professionRepository.saveAll(legacyProfessions);
    }
}
