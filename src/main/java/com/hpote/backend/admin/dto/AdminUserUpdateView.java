package com.hpote.backend.admin.dto;

import com.hpote.backend.user.dto.UserView;

public record AdminUserUpdateView(UserView user, String token) {
}
