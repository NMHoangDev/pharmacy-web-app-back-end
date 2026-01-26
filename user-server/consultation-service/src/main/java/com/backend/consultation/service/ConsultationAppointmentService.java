import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

// ...

public List<AvailableSlotResponse> getAvailableSlots(
        UUID pharmacistId,
        LocalDateTime from,
        LocalDateTime to,
        String authorization) {
    if (pharmacistId == null || from == null || to == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pharmacistId, from and to are required");
    }
    if (!from.isBefore(to)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before to");
    }

    // fetch shifts from pharmacist-service via client
    var shifts = pharmacistClient.getShifts(pharmacistId, from, to, authorization);

    // load existing bookings in range (treat CANCELLED as not occupying)
    List<ConsultationAppointment> bookings = repo.findByPharmacistIdAndStartAtBetween(pharmacistId, from, to);

    Set<LocalDateTime> occupiedStarts = new HashSet<>();
    for (ConsultationAppointment b : bookings) {
        if (!"CANCELLED".equalsIgnoreCase(b.getStatus())) {
            occupiedStarts.add(b.getStartAt());
        }
    }

    List<AvailableSlotResponse> slots = new ArrayList<>();
    final int slotMinutes = 30;

    for (var s : shifts) {
        if (s == null || !"ACTIVE".equalsIgnoreCase(s.getStatus())) {
            continue;
        }

        LocalDateTime windowStart = s.getStartAt().isBefore(from) ? from : s.getStartAt();
        LocalDateTime windowEnd = s.getEndAt().isAfter(to) ? to : s.getEndAt();

        // align to slot boundary (:00 / :30)
        LocalDateTime slotStart = alignToSlot(windowStart, slotMinutes);

        while (!slotStart.plusMinutes(slotMinutes).isAfter(windowEnd)) {
            LocalDateTime slotEnd = slotStart.plusMinutes(slotMinutes);
            boolean available = !occupiedStarts.contains(slotStart);
            slots.add(new AvailableSlotResponse(slotStart, slotEnd, available));
            slotStart = slotStart.plusMinutes(slotMinutes);
        }
    }

    return slots;
}

private LocalDateTime alignToSlot(LocalDateTime dt, int minutes) {
    if (dt == null)
        return null;
    LocalDateTime normalized = dt.withSecond(0).withNano(0);
    int mod = normalized.getMinute() % minutes;
    if (mod == 0)
        return normalized;
    int add = minutes - mod;
    return normalized.plusMinutes(add);
}
