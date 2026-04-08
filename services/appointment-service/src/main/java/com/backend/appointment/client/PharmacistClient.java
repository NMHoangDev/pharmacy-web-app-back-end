package com.backend.appointment.client;

import com.backend.appointment.client.dto.pos.PosCreateOfflineOrderRequestDto;
import com.backend.appointment.client.dto.pos.PosOfflineOrderResponseDto;
import com.backend.appointment.client.dto.pos.PosProductSearchPageDto;
import com.backend.appointment.client.dto.PharmacistPageDto;
import com.backend.appointment.client.dto.PharmacistPreviewDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.UUID;

@FeignClient(name = "pharmacist-service", url = "${services.pharmacist.url:http://pharmacist-service:7025}")
public interface PharmacistClient {

    @GetMapping("/api/pharmacists/{id}")
    PharmacistPreviewDto getPharmacist(@PathVariable("id") UUID id);

    @GetMapping("/api/pharmacists")
    PharmacistPageDto searchPharmacists(
            @RequestParam("query") String query,
            @RequestParam("page") int page,
            @RequestParam("size") int size);

    @GetMapping("/api/pharmacists/pos/products/search")
    PosProductSearchPageDto searchPosProducts(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size);

    @PostMapping("/api/pharmacists/pos/orders")
    PosOfflineOrderResponseDto createPosOrder(@RequestBody PosCreateOfflineOrderRequestDto request);
}
