package com.backend.appointment.client;

import com.backend.appointment.client.dto.UserProfileDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service", url = "${services.user.url:http://user-service:7016}")
public interface UserClient {

    @GetMapping("/api/users/{id}")
    UserProfileDto getUser(@PathVariable("id") UUID id);
}
