package com.hpote.backend.user.dto;

public record UpdateProfileResponse(UserView user, String token) {
}
