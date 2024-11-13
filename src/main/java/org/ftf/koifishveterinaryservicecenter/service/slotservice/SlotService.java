package org.ftf.koifishveterinaryservicecenter.service.slotservice;

import org.ftf.koifishveterinaryservicecenter.entity.TimeSlot;
import org.ftf.koifishveterinaryservicecenter.entity.veterinarian_slots.VeterinarianSlots;
import org.ftf.koifishveterinaryservicecenter.enums.SlotStatus;

import java.util.List;

public interface SlotService {

    List<TimeSlot> getVeterinarianSlots(Integer veterinarianId);

    TimeSlot getTimeSlotById(Integer timeSlotId);

    List<TimeSlot> getListAvailableTimeSlots();

    List<VeterinarianSlots> getVeterinarianSlotsBySlotId(Integer slotId);

    List<TimeSlot> getAvailableSlotsByVeterinarianId(Integer veterinarianId);

    void updateVeterinarianSlotsStatus(Integer veterinarianId, Integer slotId, SlotStatus status);

    List<TimeSlot> getAvailableSlotForFollowUpAppointment(Integer veterinarianId, Integer currentSlotId);

    List<TimeSlot> getBookedTimeSlots();

    List<TimeSlot> getAvailableSlots();

    VeterinarianSlots getVeterinarianSlotById(Integer veterinarianSlotId, Integer slotId);


}
