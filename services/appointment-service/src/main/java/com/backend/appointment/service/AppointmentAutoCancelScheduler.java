package com.backend.appointment.service;

import com.backend.appointment.model.AppointmentStatus;
import com.backend.appointment.repo.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentAutoCancelScheduler {

    private final AppointmentRepository appointmentRepository;

    @Value("${appointment.autocancel.grace-minutes:10}")
    private int graceMinutes;

    @Scheduled(fixedDelayString = "${appointment.autocancel.interval-ms:60000}")
    @Transactional
    public void autoCancelExpiredAppointments() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(graceMinutes);

        // Target statuses to check for expiration
        List<AppointmentStatus> targetStatuses = Arrays.asList(
                AppointmentStatus.REQUESTED,
                AppointmentStatus.PENDING,
                AppointmentStatus.CONFIRMED);

        int updatedCount = appointmentRepository.cancelExpiredAppointments(
                targetStatuses,
                threshold,
                AppointmentStatus.CANCELLED);

        if (updatedCount > 0) {
            log.info("Auto-cancelled {} expired appointments (Threshold: {})", updatedCount, threshold);
        }
    }
}
