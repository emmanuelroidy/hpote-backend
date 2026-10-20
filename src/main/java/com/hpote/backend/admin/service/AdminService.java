package com.hpote.backend.admin.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hpote.backend.admin.dto.AdminOverviewView;
import com.hpote.backend.admin.dto.ProfessionRequest;
import com.hpote.backend.common.ApiExceptionDto;
import com.hpote.backend.professional.models.Profession;
import com.hpote.backend.professional.repository.ProfessionRepository;
import com.hpote.backend.service.repository.ServiceRequestRepository;
import com.hpote.backend.user.dto.UserView;
import com.hpote.backend.user.model.User;
import com.hpote.backend.user.model.UserRole;
import com.hpote.backend.user.repository.UserRepository;
import com.hpote.backend.user.repository.UserRoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final Set<String> ALLOWED_ROLES = Set.of("NORMAL", "PROFESSIONAL", "ADMIN");

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final ProfessionRepository professionRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    @Transactional(readOnly = true)
    public AdminOverviewView overview() {
        Map<String, Long> byRole = new LinkedHashMap<>();
        for (String role : List.of("NORMAL", "PROFESSIONAL", "ADMIN")) {
            byRole.put(role, userRepository.countByRole_Name(role));
        }
        long totalProfessions = professionRepository.count();
        return new AdminOverviewView(
                userRepository.count(),
                userRepository.countByEnabled(true),
                userRepository.countByEnabled(false),
                byRole,
                totalProfessions,
                professionRepository.countByIsActiveTrue(),
                serviceRequestRepository.count());
    }

    @Transactional(readOnly = true)
    public List<UserView> users() {
        return userRepository.findAll().stream().map(UserView::from).toList();
    }

    @Transactional(readOnly = true)
    public UserView user(String rawId) {
        return UserView.from(findUser(rawId));
    }

    @Transactional
    public UserView setEnabled(String rawId, Boolean enabled, String actor) {
        if (enabled == null) {
            throw new ApiExceptionDto("enabled is required");
        }
        User user = findUser(rawId);
        if (!enabled) {
            ensureNotSelf(user, actor, "block");
        }
        if (!enabled && "ADMIN".equals(user.getRole().getName())
                && userRepository.countByRole_NameAndEnabled("ADMIN", true) <= 1) {
            throw new ApiExceptionDto("Cannot block the last enabled administrator");
        }
        user.setEnabled(enabled);
        return UserView.from(userRepository.save(user));
    }

    @Transactional
    public UserView setRole(String rawId, String rawRole, List<UUID> professionIds, String actor) {
        if (rawRole == null) {
            throw new ApiExceptionDto("role is required");
        }
        String roleName = rawRole.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(roleName)) {
            throw new ApiExceptionDto("Role must be NORMAL, PROFESSIONAL, or ADMIN");
        }
        User user = findUser(rawId);
        if ("ADMIN".equals(user.getRole().getName()) && !"ADMIN".equals(roleName)) {
            ensureNotSelf(user, actor, "demote");
            if (userRepository.countByRole_Name("ADMIN") <= 1
                    || (user.isEnabled()
                    && userRepository.countByRole_NameAndEnabled("ADMIN", true) <= 1)) {
                throw new ApiExceptionDto("Cannot demote the last enabled administrator");
            }
        }
        UserRole role = userRoleRepository.findByName(roleName)
                .orElseThrow(() -> new ApiExceptionDto("Role not found: " + roleName));
        user.setRole(role);
        if (!"PROFESSIONAL".equals(roleName)) {
            user.getProfessions().clear();
        } else if (professionIds != null) {
            user.setProfessions(resolveActiveProfessions(professionIds));
        } else if (user.getProfessions().isEmpty()) {
            throw new ApiExceptionDto("A professional must have at least one profession");
        }
        return UserView.from(userRepository.save(user));
    }

    @Transactional
    public UserView setProfessions(String rawId, List<UUID> ids) {
        User user = findUser(rawId);
        if (!"PROFESSIONAL".equals(user.getRole().getName())) {
            throw new ApiExceptionDto("Only professional users can be assigned professions");
        }
        user.setProfessions(resolveActiveProfessions(ids));
        return UserView.from(userRepository.save(user));
    }

    private Set<Profession> resolveActiveProfessions(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ApiExceptionDto("A professional must have at least one profession");
        }
        if (ids.stream().distinct().count() != ids.size()) {
            throw new ApiExceptionDto("Duplicate profession IDs are not allowed");
        }
        Set<String> professionIds = ids.stream().map(UUID::toString).collect(Collectors.toSet());
        List<Profession> professions = professionRepository.findAllById(professionIds);
        if (professions.size() != professionIds.size()) {
            throw new ApiExceptionDto("One or more profession IDs are invalid");
        }
        if (professions.stream().anyMatch(profession -> !Boolean.TRUE.equals(profession.getIsActive()))) {
            throw new ApiExceptionDto("Inactive professions cannot be assigned");
        }
        return professions.stream().collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public List<Profession> professions() {
        return professionRepository.findAll();
    }

    @Transactional
    public Profession createProfession(ProfessionRequest request) {
        ensureUniqueProfessionName(request.name(), null);
        return professionRepository.save(Profession.builder()
                .name(request.name().trim())
                .description(request.description())
                .isActive(request.isActive() == null || request.isActive())
                .build());
    }

    @Transactional
    public Profession updateProfession(String rawId, ProfessionRequest request) {
        Profession profession = findProfession(rawId);
        ensureUniqueProfessionName(request.name(), profession.getId());
        profession.setName(request.name().trim());
        profession.setDescription(request.description());
        if (request.isActive() != null) {
            profession.setIsActive(request.isActive());
        }
        return professionRepository.save(profession);
    }

    @Transactional
    public Profession setProfessionActive(String rawId, Boolean active) {
        if (active == null) {
            throw new ApiExceptionDto("isActive is required");
        }
        Profession profession = findProfession(rawId);
        profession.setIsActive(active);
        return professionRepository.save(profession);
    }

    private User findUser(String rawId) {
        String id = normalizedId(rawId, "user");
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiExceptionDto("User not found"));
    }

    private Profession findProfession(String rawId) {
        String id = normalizedId(rawId, "profession");
        return professionRepository.findById(id)
                .orElseThrow(() -> new ApiExceptionDto("Profession not found"));
    }

    private String normalizedId(String rawId, String kind) {
        try {
            UUID id = UUID.fromString(rawId);
            if (!id.toString().equalsIgnoreCase(rawId)) {
                throw new IllegalArgumentException();
            }
            return id.toString();
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ApiExceptionDto("Invalid " + kind + " ID");
        }
    }

    private void ensureUniqueProfessionName(String name, String excludingId) {
        String normalized = name.trim();
        professionRepository.findByNameIgnoreCase(normalized).ifPresent(existing -> {
            if (!existing.getId().equals(excludingId)) {
                throw new ApiExceptionDto("Profession name already exists");
            }
        });
    }

    private void ensureNotSelf(User user, String actor, String action) {
        if (actor != null && (actor.equalsIgnoreCase(user.getEmail()) || actor.equals(user.getPhone()))) {
            throw new ApiExceptionDto("You cannot " + action + " your own account");
        }
    }
}
