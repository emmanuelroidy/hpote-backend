package com.hpote.backend.user.dto;

import com.hpote.backend.user.model.User;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserView {
    String id;
    String name;
    String phone;
    String email;
    String role;
    String profilePhoto;
    Double rating;

    public static UserView from(User user) {
        return UserView.builder()
                .id(user.getId())
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .profilePhoto(user.getProfilePhoto())
                .rating(user.getRating())
                .build();
    }
}
