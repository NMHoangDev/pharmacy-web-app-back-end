package com.backend.appointment.service;

import com.backend.appointment.model.Appointment;
import com.backend.appointment.model.AppointmentStatus;
import com.backend.appointment.repo.AppointmentRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentNoShowScheduler {

    private final AppointmentRepository appointmentRepository;

    @Value("${appointment.no-show.grace-minutes:15}")
    private int graceMinutes;

    @Scheduled(fixedDelayString = "${appointment.no-show.interval-ms:60000}")
    @Transactional
    public void markNoShowAppointments() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(graceMinutes);
        List<AppointmentStatus> statuses = List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS);
        List<Appointment> candidates = appointmentRepository.findByStatusInAndEndAtBefore(statuses, threshold);
        if (candidates.isEmpty()) {
            return;
        }
        for (Appointment appointment : candidates) {
            appointment.setStatus(AppointmentStatus.NO_SHOW);
        }
        log.info("Auto marked {} appointments as NO_SHOW (threshold={})", candidates.size(), threshold);
    }
}