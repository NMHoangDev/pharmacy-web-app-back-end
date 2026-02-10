package com.backend.appointment.client;

import com.backend.appointment.client.dto.PharmacistPreviewDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

@FeignClient(name = "pharmacist-service", url = "http://localhost:7025")
public interface PharmacistClient {

    @GetMapping("/api/pharmacists/{id}")
    PharmacistPreviewDto getPharmacist(@PathVariable("id") UUID id);
}
