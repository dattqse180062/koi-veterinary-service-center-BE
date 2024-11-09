package org.ftf.koifishveterinaryservicecenter.service.appointmentservice.appointmentstate;

import org.ftf.koifishveterinaryservicecenter.entity.Appointment;
import org.ftf.koifishveterinaryservicecenter.entity.TimeSlot;
import org.ftf.koifishveterinaryservicecenter.entity.User;
import org.ftf.koifishveterinaryservicecenter.enums.AppointmentStatus;
import org.ftf.koifishveterinaryservicecenter.enums.PaymentMethod;
import org.ftf.koifishveterinaryservicecenter.enums.PaymentStatus;
import org.ftf.koifishveterinaryservicecenter.enums.SlotStatus;
import org.ftf.koifishveterinaryservicecenter.exception.AppointmentUpdatedException;
import org.ftf.koifishveterinaryservicecenter.exception.IllegalStateException;
import org.ftf.koifishveterinaryservicecenter.repository.AppointmentRepository;
import org.ftf.koifishveterinaryservicecenter.service.slotservice.SlotService;
import org.ftf.koifishveterinaryservicecenter.service.statusservice.StatusService;
import org.ftf.koifishveterinaryservicecenter.service.userservice.AuthenticationService;
import org.ftf.koifishveterinaryservicecenter.service.userservice.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PendingState implements AppointmentState {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final AppointmentRepository appointmentRepository;
    private final SlotService slotService;
    private final StatusService statusService;

    public PendingState(AuthenticationService authenticationService, UserService userService, AppointmentRepository appointmentRepository, SlotService slotService, StatusService statusService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.appointmentRepository = appointmentRepository;
        this.slotService = slotService;
        this.statusService = statusService;
    }

    @Override
    @Transactional
    public void updateState(Appointment appointment) throws AppointmentUpdatedException {
        String roleKey = authenticationService.getAuthenticatedUserRoleKey();

        if (!roleKey.equals("STA"))
            throw new IllegalStateException("Only Staff can update appointments from PENDING to CONFIRMED");

        if (appointment.getPayment().getPaymentMethod().equals(PaymentMethod.VN_PAY) && appointment.getPayment().getStatus().equals(PaymentStatus.NOT_PAID))
            throw new AppointmentUpdatedException("You can not update CONFIRMED of un-paid appointment");

        // set new status for the appointment
        appointment.setCurrentStatus(AppointmentStatus.CONFIRMED);

//        // Update Veterinarian_Slot status
//        TimeSlot timeSlot = slotService.getTimeSlotById(appointment.getTimeSlot().getSlotId());
//        slotService.updateVeterinarianSlotsStatus(appointment.getVeterinarian().getUserId(), timeSlot.getSlotId(), SlotStatus.BOOKED);

        // get staff Id from authenticated User in order to log
        Integer staffId = authenticationService.getAuthenticatedUserId();
        User staff = userService.getUserProfile(staffId);

        // Staff log confirmed
        statusService.staffLogConfirmedAppointment(appointment, staff);
        appointmentRepository.save(appointment);
    }


}
