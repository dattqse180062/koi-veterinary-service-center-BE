package org.ftf.koifishveterinaryservicecenter.repository;

import org.ftf.koifishveterinaryservicecenter.entity.Payment;
import org.ftf.koifishveterinaryservicecenter.enums.PaymentMethod;
import org.ftf.koifishveterinaryservicecenter.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    @Query("SELECT p FROM Payment p JOIN Appointment a ON p.paymentId = a.payment.paymentId WHERE a.appointmentId = :appointmentId")
    Payment findByAppointmentId(Integer appointmentId);

    // PaymentRepository.java

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.transactionTime BETWEEN :startDate AND :endDate")
    long countPaymentsInRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.transactionTime BETWEEN :startDate AND :endDate")
    Double sumTotalAmountInRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentMethod = :method AND p.transactionTime BETWEEN :startDate AND :endDate")
    long countByPaymentMethodInRange(@Param("method") PaymentMethod method,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status AND p.transactionTime BETWEEN :startDate AND :endDate")
    long countByStatusInRange(@Param("status") PaymentStatus status,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate);
  }