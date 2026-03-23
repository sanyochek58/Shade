package com.vpn.billing.repository;

import com.vpn.billing.entity.Payment;
import com.vpn.billing.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentsRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Payment> findByTelegramChargeId(String telegramChargeId);

    Optional<Payment> findTopByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            PaymentStatus status
    );
}
