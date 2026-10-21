package com.hpote.backend.admin;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.hpote.backend.admin.service.AdminService;
import com.hpote.backend.common.ApiExceptionDto;
import com.hpote.backend.common.JwtUtil;
import com.hpote.backend.admin.dto.UpdateAdminUserRequest;
import com.hpote.backend.professional.repository.ProfessionRepository;
import com.hpote.backend.service.repository.ServiceRequestRepository;
import com.hpote.backend.user.model.User;
import com.hpote.backend.user.model.UserRole;
import com.hpote.backend.user.repository.UserRepository;
import com.hpote.backend.user.repository.UserRoleRepository;

import java.util.List;
import java.util.HashSet;

class AdminServiceTest {

    private static final String ADMIN_ID = "00000000-0000-0000-0000-000000000001";
    private UserRepository userRepository;
    private UserRoleRepository userRoleRepository;
    private JwtUtil jwtUtil;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        jwtUtil = mock(JwtUtil.class);
        adminService = new AdminService(userRepository, userRoleRepository,
                mock(ProfessionRepository.class), mock(ServiceRequestRepository.class), jwtUtil);
    }

    @Test
    void preventsAdminFromBlockingThemself() {
        User administrator = user(ADMIN_ID, "ADMIN", "admin@example.test");
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrator));

        assertThrows(ApiExceptionDto.class,
                () -> adminService.setEnabled(ADMIN_ID, false, "admin@example.test"));
        verifyNoInteractionsExceptLookup();
    }

    @Test
    void preventsBlockingLastEnabledAdministrator() {
        User administrator = user(ADMIN_ID, "ADMIN", "admin@example.test");
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrator));
        when(userRepository.countByRole_NameAndEnabled("ADMIN", true)).thenReturn(1L);

        assertThrows(ApiExceptionDto.class,
                () -> adminService.setEnabled(ADMIN_ID, false, "other@example.test"));
    }

    @Test
    void rejectsUnknownRoleBeforeLookingUpUser() {
        assertThrows(ApiExceptionDto.class,
                () -> adminService.setRole(ADMIN_ID, "ROOT", null, "admin@example.test"));
        verifyNoInteractions(userRepository);
    }

    @Test
    void updatesAdministratorContactDetailsAndReturnsRefreshedToken() {
        UserRole role = UserRole.builder().name("ADMIN").build();
        User administrator = user(ADMIN_ID, "ADMIN", "old@example.test");
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(administrator));
        when(userRepository.findByPhone("555-0100")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.test")).thenReturn(Optional.empty());
        when(userRoleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));
        when(userRepository.save(administrator)).thenReturn(administrator);
        when(jwtUtil.generateToken(administrator)).thenReturn("refreshed-token");

        var result = adminService.updateUser(ADMIN_ID,
                new UpdateAdminUserRequest("Updated Name", "555-0100",
                        "new@example.test", "ADMIN", List.of()),
                "old@example.test");

        org.junit.jupiter.api.Assertions.assertEquals("Updated Name", result.user().getName());
        org.junit.jupiter.api.Assertions.assertEquals("555-0100", result.user().getPhone());
        org.junit.jupiter.api.Assertions.assertEquals("new@example.test", result.user().getEmail());
        org.junit.jupiter.api.Assertions.assertEquals("refreshed-token", result.token());
    }

    private void verifyNoInteractionsExceptLookup() {
        org.mockito.Mockito.verify(userRepository).findById(ADMIN_ID);
        org.mockito.Mockito.verifyNoMoreInteractions(userRepository);
    }

    private User user(String id, String roleName, String email) {
        UserRole role = UserRole.builder().name(roleName).build();
        User user = User.builder().name("Administrator").email(email).phone("555")
                .role(role).enabled(true).professions(new HashSet<>()).build();
        user.setId(id);
        return user;
    }
}
