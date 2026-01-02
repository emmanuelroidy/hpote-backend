package com.hpote.backend.service.service;
import java.util.Comparator;
import java.util.List;
import com.hpote.backend.professional.models.ProfessionalProfile;
import com.hpote.backend.professional.models.Profession;
import com.hpote.backend.professional.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final ProfessionalRepository professionalRepository;

    public List<ProfessionalProfile> findNearest(
            Profession profession,
            double lat,
            double lng
    ) {
        List<ProfessionalProfile> profiles = professionalRepository.findAll()
                .stream()
                .filter(p -> p.getProfession().equals(profession))
                .filter(ProfessionalProfile::getIsAvailable)
                .sorted(Comparator.comparingDouble(
                        p -> distance(
                                p.getLatitude(),
                                p.getLongitude(),
                                lat,
                                lng
                        )
                ))
                .toList();

        return profiles;
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        return R * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }
}
