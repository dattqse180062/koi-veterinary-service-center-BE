package org.ftf.koifishveterinaryservicecenter.service.appointmentservice.appointmentstate;

import org.ftf.koifishveterinaryservicecenter.entity.Appointment;
import org.ftf.koifishveterinaryservicecenter.entity.User;
import org.ftf.koifishveterinaryservicecenter.enums.AppointmentStatus;
import org.ftf.koifishveterinaryservicecenter.exception.IllegalStateException;
import org.ftf.koifishveterinaryservicecenter.repository.AppointmentRepository;
import org.ftf.koifishveterinaryservicecenter.service.statusservice.StatusService;
import org.ftf.koifishveterinaryservicecenter.service.userservice.AuthenticationService;
import org.ftf.koifishveterinaryservicecenter.service.userservice.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OngoingState implements AppointmentState {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final AppointmentRepository appointmentRepository;
    private final StatusService statusService;

    public OngoingState(AuthenticationService authenticationService, UserService userService, AppointmentRepository appointmentRepository, StatusService statusService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.appointmentRepository = appointmentRepository;
        this.statusService = statusService;
    }

    @Override
    public void updateState(Appointment appointment) {
        String roleKey = authenticationService.getAuthenticatedUserRoleKey();

        if (roleKey.equals("STA") || roleKey.equals("VET")) {
            appointment.setCurrentStatus(AppointmentStatus.CHECKED_IN);

            // get checkin Actor
            Integer authenticatedUserId = authenticationService.getAuthenticatedUserId();
            User checkinActor = userService.getUserProfile(authenticatedUserId);

            // staff/veterinarian logs checked-in
            statusService.actorLogCheckInAppointment(appointment, checkinActor);
            appointmentRepository.save(appointment);
        } else
            throw new IllegalStateException("Only Staff/Veterinarian can update appointments from ON_GOING to CHECKED_IN");
    }

    public boolean isOnTimeToCheckin(Appointment appointment) {
        return LocalDateTime.now().isAfter(appointment.getTimeSlot().getDateTimeBasedOnSlot());
    }


}