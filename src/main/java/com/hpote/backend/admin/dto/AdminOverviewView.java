package com.hpote.backend.admin.dto;

import java.util.Map;

public record AdminOverviewView(
        long users,
        long enabledUsers,
        long blockedUsers,
        Map<String, Long> usersByRole,
        long professions,
        long activeProfessions,
        long serviceRequests) {
}
