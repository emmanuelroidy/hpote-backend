package com.hpote.backend.admin.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hpote.backend.admin.dto.AdminOverviewView;
import com.hpote.backend.admin.dto.ProfessionRequest;
import com.hpote.backend.admin.dto.UpdateUserProfessionsRequest;
import com.hpote.backend.admin.dto.UpdateUserRoleRequest;
import com.hpote.backend.admin.dto.UpdateUserStatusRequest;
import com.hpote.backend.admin.service.AdminService;
import com.hpote.backend.common.ApiResponseDto;
import com.hpote.backend.professional.models.Profession;
import com.hpote.backend.service.dto.ServiceRequestView;
import com.hpote.backend.service.service.ServiceMarketplaceService;
import com.hpote.backend.user.dto.UserView;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Validated
public class AdminController {

    private final AdminService adminService;
    private final ServiceMarketplaceService marketplaceService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponseDto<AdminOverviewView>> overview() {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Admin overview", adminService.overview()));
    }

    @GetMapping("/service-requests")
    public ResponseEntity<ApiResponseDto<List<ServiceRequestView>>> serviceRequests() {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Service requests retrieved",
                marketplaceService.getAllRequestsForAdmin()));
    }

    @PostMapping("/service-requests/{requestId}/cancel")
    public ResponseEntity<ApiResponseDto<ServiceRequestView>> cancelServiceRequest(
            @PathVariable String requestId) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Service request cancelled",
                marketplaceService.cancelRequestForAdmin(requestId)));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponseDto<List<UserView>>> users() {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Users retrieved", adminService.users()));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponseDto<UserView>> user(@PathVariable String id) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "User retrieved", adminService.user(id)));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponseDto<UserView>> updateStatus(
            @PathVariable String id, @Valid @RequestBody UpdateUserStatusRequest request, Principal principal) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "User status updated",
                adminService.setEnabled(id, request.enabled(), principal.getName())));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponseDto<UserView>> updateRole(
            @PathVariable String id, @Valid @RequestBody UpdateUserRoleRequest request, Principal principal) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "User role updated",
                adminService.setRole(id, request.role(), request.professionIds(), principal.getName())));
    }

    @PutMapping("/users/{id}/professions")
    public ResponseEntity<ApiResponseDto<UserView>> updateProfessions(
            @PathVariable String id, @Valid @RequestBody UpdateUserProfessionsRequest request) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "User professions updated",
                adminService.setProfessions(id, request.professionIds())));
    }

    @GetMapping("/professions")
    public ResponseEntity<ApiResponseDto<List<Profession>>> professions() {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Professions retrieved",
                adminService.professions()));
    }

    @PostMapping("/professions")
    public ResponseEntity<ApiResponseDto<Profession>> createProfession(
            @Valid @RequestBody ProfessionRequest request) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Profession created",
                adminService.createProfession(request)));
    }

    @PutMapping("/professions/{id}")
    public ResponseEntity<ApiResponseDto<Profession>> updateProfession(
            @PathVariable String id, @Valid @RequestBody ProfessionRequest request) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Profession updated",
                adminService.updateProfession(id, request)));
    }

    @PatchMapping("/professions/{id}/status")
    public ResponseEntity<ApiResponseDto<Profession>> updateProfessionStatus(
            @PathVariable String id, @RequestBody UpdateProfessionStatusRequest request) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Profession status updated",
                adminService.setProfessionActive(id, request.isActive())));
    }

    @DeleteMapping("/professions/{id}")
    public ResponseEntity<ApiResponseDto<Profession>> deleteProfession(@PathVariable String id) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Profession deactivated",
                adminService.setProfessionActive(id, false)));
    }

    public record UpdateProfessionStatusRequest(Boolean isActive) {
    }
}
