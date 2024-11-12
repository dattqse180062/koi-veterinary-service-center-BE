package org.ftf.koifishveterinaryservicecenter.email;

import org.ftf.koifishveterinaryservicecenter.entity.*;
import org.ftf.koifishveterinaryservicecenter.enums.AppointmentStatus;
import org.ftf.koifishveterinaryservicecenter.enums.PaymentMethod;
import org.ftf.koifishveterinaryservicecenter.repository.PaymentRepository;
import org.ftf.koifishveterinaryservicecenter.repository.ServiceRepository;
import org.ftf.koifishveterinaryservicecenter.repository.TimeSlotRepository;
import org.ftf.koifishveterinaryservicecenter.repository.UserRepository;
import org.ftf.koifishveterinaryservicecenter.service.emailservice.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = true)
public class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private PaymentRepository paymentRepository;


    @Test
    public void testSendEmail() throws Exception {
        String to = "crisbrian070503@gmail.com";
        String subject = "OTP Test";
        String message = "OTP is 1234";

        emailService.sendTextEmail(to, subject, message);
    }


    @Test
    public void testSendAppointmentBills() throws Exception {
        LocalDateTime date = LocalDateTime.now();
        Optional<Service> service = serviceRepository.findById(1);
        Address address = Address.builder().city("Ho Chi Minh").district("Quan 1").ward("Phuong Ben Nghe").homeNumber("88").build();
        TimeSlot savedTimeSlot = timeSlotRepository.findById(245).get();


        User customer = userRepository.findUsersByUserId(6);
        User veterinarian = userRepository.findVeterinarianById(13);
        String email = "crisbrian070503@gmail.com";
        String phone = "0987654321";
        String customerName = "My name is Demo";
        String description = "My description";
        BigDecimal totalPrice = service.get().getServicePrice();

        Payment payment = new Payment();
        payment.setAmount(totalPrice);
        payment.setPaymentMethod(PaymentMethod.CASH);

        Payment savedPayment = paymentRepository.save(payment);

        Appointment newAppointment = new Appointment();
        newAppointment.setCreatedDate(date);
        newAppointment.setService(service.get());
        newAppointment.setTimeSlot(savedTimeSlot);
        newAppointment.setCustomer(customer);
        newAppointment.setVeterinarian(veterinarian);
        newAppointment.setEmail(email);
        newAppointment.setPhoneNumber(phone);
        newAppointment.setCurrentStatus(AppointmentStatus.PENDING);
        newAppointment.setCustomerName(customerName);
        newAppointment.setDescription(description);
        newAppointment.setTotalPrice(totalPrice);
        newAppointment.setPayment(savedPayment);

        String toEmail = "crisbrian070503@gmail.com";
        String subject = "Koi Fish - Appointment Bills";

        emailService.sendAppointmentBills(toEmail, subject, newAppointment);
    }


}
