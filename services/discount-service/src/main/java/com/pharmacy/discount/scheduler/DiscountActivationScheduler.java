package com.pharmacy.discount.scheduler;

import com.pharmacy.discount.entity.Discount;
import com.pharmacy.discount.entity.DiscountStatus;
import com.pharmacy.discount.kafka.DiscountNotificationPublisher;
import com.pharmacy.discount.repository.DiscountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DiscountActivationScheduler {

    private static final Logger log = LoggerFactory.getLogger(DiscountActivationScheduler.class);

    private final DiscountRepository discountRepository;
    private final DiscountNotificationPublisher notificationPublisher;

    public DiscountActivationScheduler(
            DiscountRepository discountRepository,
            DiscountNotificationPublisher notificationPublisher) {
        this.discountRepository = discountRepository;
        this.notificationPublisher = notificationPublisher;
    }

    @Scheduled(cron = "${discount.activation.cron:0 */1 * * * *}")
    @Transactional
    public void activateScheduledDiscounts() {
        LocalDateTime now = LocalDateTime.now();

        List<Discount> ready = discountRepository
                .findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(DiscountStatus.SCHEDULED, now, now);
        if (ready.isEmpty()) {
            return;
        }

        for (Discount discount : ready) {
            discount.setStatus(DiscountStatus.ACTIVE);
        }
        discountRepository.saveAll(ready);

        for (Discount discount : ready) {
            notificationPublisher.publishActivatedSafe(discount);
        }

        log.info("Activated {} scheduled discount(s)", ready.size());
    }
}
