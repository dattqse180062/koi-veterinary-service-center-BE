package org.ftf.koifishveterinaryservicecenter.mapper;

import org.ftf.koifishveterinaryservicecenter.dto.appointment.AppointmentDetailsDto;
import org.ftf.koifishveterinaryservicecenter.dto.appointment.AppointmentDto;
import org.ftf.koifishveterinaryservicecenter.dto.appointment.AppointmentForListDto;
import org.ftf.koifishveterinaryservicecenter.entity.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {
        PaymentMapper.class,
        AddressMapper.class,
        TimeSlotMapper.class
})
public interface AppointmentMapper {

    AppointmentMapper INSTANCE = Mappers.getMapper(AppointmentMapper.class);

    // Map Appointment to AppointmentDto
    // Usage: mapping from DTO as body request of creating appointment
    @Mapping(source = "serviceId", target = "service.serviceId")
    @Mapping(source = "veterinarianId", target = "veterinarian.userId", conditionExpression = "java(appointmentDto.getVeterinarianId() != null)")
    @Mapping(source = "addressId", target = "address.addressId", conditionExpression = "java(appointmentDto.getAddressId() != null)")
    @Mapping(source = "slotId", target = "timeSlot.slotId")
    @Mapping(source = "customerName", target = "customerName")
    @Mapping(source = "phoneNumber", target = "phoneNumber")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "fishId", target = "fish.fishId", conditionExpression = "java(appointmentDto.getFishId() != null)")
    @Mapping(source = "voucherId", target = "voucher.id", conditionExpression = "java(appointmentDto.getVoucherId() != null)")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "payment", target = "payment", qualifiedByName = "convertToEntity")
    Appointment convertedToAppointment(AppointmentDto appointmentDto);


    @Mapping(target = "service.description", ignore = true)
    @Mapping(source = "currentStatus", target = "currentStatus")
    //@Mapping(source = "timeSlot.slotId", target = "slotId")
    @Mapping(source = "timeSlot", target = "timeSlot", qualifiedByName = "convertToAvailableTimeSlotDto")
    @Mapping(source = "followUpAppointment.appointmentId", target = "followUpAppointmentId")
    @Mapping(source = "feedback.feedbackId", target = "feedbackId")
    @Mapping(source = "voucher.discountAmount", target = "discount")
    @Mapping(source = "customer.userId", target = "customerId")
    @Mapping(target = "veterinarian.username", ignore = true)
    @Mapping(target = "veterinarian.password", ignore = true)
    @Mapping(target = "veterinarian.avatar", ignore = true)
    @Mapping(target = "veterinarian.address", ignore = true)
    @Mapping(target = "veterinarian.phoneNumber", ignore = true)
    AppointmentDetailsDto convertedToAppointmentDetailsDto(Appointment appointment);

    @Mapping(target = "service.description", ignore = true)
    @Mapping(source = "currentStatus", target = "currentStatus")
    @Mapping(source = "followUpAppointment.appointmentId", target = "followUpAppointmentId")
    @Mapping(target = "veterinarian", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "service.servicePrice", ignore = true)
    @Mapping(target = "movingSurcharge", ignore = true)
    AppointmentDetailsDto convertedToAppointmentDetailsDtoForVet(Appointment appointment);


    // Mapper for viewing appointment list
    @Mapping(target = "serviceName", source = "service.serviceName")
    @Mapping(target = "veterinarianName", source = "veterinarian.firstName")
    @Mapping(target = "paymentStatus", source = "payment.status")
    @Mapping(target = "appointmentStatus", source = "currentStatus")
    @Mapping(target = "timeSlot.appointment", ignore = true)
    AppointmentForListDto convertedToAppointmentDtoForList(Appointment appointment);


}
