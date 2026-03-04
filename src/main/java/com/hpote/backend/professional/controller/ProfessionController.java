package com.hpote.backend.professional.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import com.hpote.backend.common.ApiResponseDto;
import com.hpote.backend.professional.models.Profession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import com.hpote.backend.professional.service.ProfessionService;

@RestController
public class ProfessionController {

    @Autowired
    private ProfessionService professionService;

    @GetMapping("/api/professions")
    public ResponseEntity<ApiResponseDto<List<Profession>>> getAllProfessions() {
        List<Profession> professions = professionService.getAllProfessions();
        return ResponseEntity.ok(new ApiResponseDto<List<Profession>>(true, 200, "Professions retrieved successfully", professions));
    }
}
