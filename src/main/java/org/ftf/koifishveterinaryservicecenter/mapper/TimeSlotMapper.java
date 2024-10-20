package org.ftf.koifishveterinaryservicecenter.mapper;

import org.ftf.koifishveterinaryservicecenter.dto.TimeSlotDto;
import org.ftf.koifishveterinaryservicecenter.dto.appointment.AppointmentFeedbackDto;
import org.ftf.koifishveterinaryservicecenter.entity.Appointment;
import org.ftf.koifishveterinaryservicecenter.entity.TimeSlot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface TimeSlotMapper {

    TimeSlotMapper INSTANCE = Mappers.getMapper(TimeSlotMapper.class);

    // Map TimeSlot to TimeSlotDto, using a custom method to map the first appointment
    @Named("convertToTimeSlotDto")
    @Mapping(source = "appointments", target = "appointment", qualifiedByName = "mapFirstAppointment")
//    @Mapping(target = "appointment.timeSlot", ignore = true)
    TimeSlotDto convertToTimeSlotDto(TimeSlot timeSlot);

    // A custom method to extract the first appointment from the set and map it to AppointmentDto
    @Named("mapFirstAppointment")
    default AppointmentFeedbackDto mapFirstAppointment(Set<Appointment> appointments) {
        Appointment firstAppointment = appointments.stream().findFirst().get();
        AppointmentFeedbackDto appointmentFeedbackDto = new AppointmentFeedbackDto();
        appointmentFeedbackDto.setAppointmentId(firstAppointment.getAppointmentId());
        appointmentFeedbackDto.setServiceName(firstAppointment.getService().getServiceName());
        appointmentFeedbackDto.setCurrentStatus(firstAppointment.getCurrentStatus());
        return appointmentFeedbackDto;
    }

    @Named("convertToTimeSlotDtoAvailable")
    @Mappings({
            @Mapping(source = "slotOrder", target = "slotOrder"),
            @Mapping(source = "day", target = "day"),
            @Mapping(source = "month", target = "month"),
            @Mapping(source = "year", target = "year"),
            @Mapping(source = "slotId", target = "slotId"),
            @Mapping(source = "description", target = "description")
    })
    TimeSlotDto convertToTimeSlotDtoAvailable(TimeSlot timeSlotDto);

    @Named("convertToAvailableTimeSlotDto")
    @Mapping(target = "appointment", ignore = true)
    TimeSlotDto convertToAvailableTimeSlotDto(TimeSlot timeSlot);

}
