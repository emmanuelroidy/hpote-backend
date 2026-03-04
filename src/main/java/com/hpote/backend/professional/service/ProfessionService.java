package com.hpote.backend.professional.service;

import java.util.List;

import com.hpote.backend.professional.models.Profession;
import com.hpote.backend.professional.repository.ProfessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfessionService {

    private final ProfessionRepository professionRepository;

    public List<Profession> getAllProfessions() {
        return professionRepository.findAll();
    }

}
