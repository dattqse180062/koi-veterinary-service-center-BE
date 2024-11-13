package org.ftf.koifishveterinaryservicecenter.service.slotservice;

import org.ftf.koifishveterinaryservicecenter.entity.TimeSlot;
import org.ftf.koifishveterinaryservicecenter.entity.User;
import org.ftf.koifishveterinaryservicecenter.entity.veterinarian_slots.VeterinarianSlotId;
import org.ftf.koifishveterinaryservicecenter.entity.veterinarian_slots.VeterinarianSlots;
import org.ftf.koifishveterinaryservicecenter.enums.SlotStatus;
import org.ftf.koifishveterinaryservicecenter.exception.TimeSlotNotFoundException;
import org.ftf.koifishveterinaryservicecenter.exception.UserNotFoundException;
import org.ftf.koifishveterinaryservicecenter.repository.TimeSlotRepository;
import org.ftf.koifishveterinaryservicecenter.repository.VeterinarianSlotsRepository;
import org.ftf.koifishveterinaryservicecenter.service.userservice.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SlotServiceImpl implements SlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final UserService userService;
    private final VeterinarianSlotsRepository veterinarianSlotsRepository;

    @Autowired
    public SlotServiceImpl(TimeSlotRepository timeSlotRepository, UserService userService, VeterinarianSlotsRepository veterinarianSlotsRepository) {
        this.timeSlotRepository = timeSlotRepository;
        this.userService = userService;
        this.veterinarianSlotsRepository = veterinarianSlotsRepository;
    }

    @Override
    public List<TimeSlot> getVeterinarianSlots(Integer veterinarianId) {
        User veterinarian = userService.getVeterinarianById(veterinarianId);

        // Veterinarian existed
        List<TimeSlot> veterinarianSlotsList = timeSlotRepository.findByVeterinarianId(veterinarianId);
        if (veterinarianSlotsList.isEmpty()) { // Veterinarian has no slots
            throw new TimeSlotNotFoundException("Veterinarian with ID: " + veterinarianId + " has no shift schedule yet");
        } // Veterinarian having slots

        // Filter appointments to retain only the one with matching veterinarianId
        for (TimeSlot timeSlot : veterinarianSlotsList) {
            timeSlot.setAppointments(timeSlot.getAppointments().stream().filter(appointment -> appointment.getVeterinarian().getUserId().equals(veterinarianId)).limit(1)  // Keep only one appointment with the matching veterinarianId
                    .collect(Collectors.toSet()));
        }

        veterinarianSlotsList = veterinarianSlotsList.stream() // Keep only TimeSlots have appointments
                .filter(timeSlot -> !timeSlot.getAppointments().isEmpty())
                .collect(Collectors.toList());

        return veterinarianSlotsList;
    }

    @Override
    public TimeSlot getTimeSlotById(Integer timeSlotId) {
        Optional<TimeSlot> timeSlot = timeSlotRepository.findById(timeSlotId);
        if (timeSlot.isEmpty()) throw new TimeSlotNotFoundException("Time slot with ID: " + timeSlotId + " not found");
        return timeSlot.get();
    }

    @Override
    public List<TimeSlot> getListAvailableTimeSlots() {

        LocalDateTime threeHoursFromNow = LocalDateTime.now().plusHours(3);
        LocalDateTime threeMonthsFromNow = LocalDateTime.now().plusMonths(3);

        List<TimeSlot> availableTimeSlot = timeSlotRepository.getAvailableTimeSlot();
        return availableTimeSlot.stream().filter(timeSlot -> timeSlot.getDateTimeBasedOnSlot().isAfter(threeHoursFromNow) && timeSlot.getDateTimeBasedOnSlot().isBefore(threeMonthsFromNow) && hasAvailableVeterinarian(timeSlot.getSlotId())).toList();

    }

    @Override
    public List<VeterinarianSlots> getVeterinarianSlotsBySlotId(Integer slotId) {
        return veterinarianSlotsRepository.getAvailableSlotsBySlotId(SlotStatus.AVAILABLE, slotId);
    }

    private boolean hasAvailableVeterinarian(Integer slotId) {
        return !getVeterinarianSlotsBySlotId(slotId).isEmpty();
    }


    @Override
    public List<TimeSlot> getAvailableSlotsByVeterinarianId(Integer veterinarianId) throws UserNotFoundException {
        User veterinarian = userService.getVeterinarianById(veterinarianId);

        LocalDateTime currentDate = LocalDateTime.now();
        LocalDateTime threeHoursFromNow = currentDate.plusHours(3);

        Integer nextThreeHour = threeHoursFromNow.getHour();

        Integer beginSlot = 1;
        if (nextThreeHour <= 10) {
            beginSlot = 2;
        } else if (nextThreeHour <= 13) {
            beginSlot = 3;
        } else if (nextThreeHour <= 15) {
            beginSlot = 4;
        } else {
            currentDate = currentDate.plusDays(1);
        }

        Integer currentYear = currentDate.getYear();
        Integer currentMonth = currentDate.getMonthValue();
        Integer currentDay = currentDate.getDayOfMonth();

        LocalDateTime endDate = currentDate.plusDays(30);
        Integer endYear = endDate.getYear();
        Integer endMonth = endDate.getMonthValue();
        Integer endDay = endDate.getDayOfMonth();

        List<TimeSlot> timeSlots = timeSlotRepository.findAvailableTimeSlotByVeterinarianId(veterinarian.getUserId(), currentYear, currentMonth, currentDay, beginSlot, endYear, endMonth, endDay);
        if (timeSlots.isEmpty()) {
            throw new TimeSlotNotFoundException("There are no available slots");
        }
        return timeSlots;
    }

    @Override
    public void updateVeterinarianSlotsStatus(Integer veterinarianId, Integer slotId, SlotStatus status) {
        VeterinarianSlotId veterinarianSlotId = new VeterinarianSlotId();
        veterinarianSlotId.setSlotId(slotId);
        veterinarianSlotId.setVeterinarianId(veterinarianId);

        //  VeterinarianSlots veterinarianSlots = veterinarianSlotsRepository.findById(veterinarianSlotId).get();

        Optional<VeterinarianSlots> veterinarianSlots = veterinarianSlotsRepository.findById(veterinarianSlotId);
        veterinarianSlots.ifPresent(slots -> {
            slots.setStatus(status);
            veterinarianSlotsRepository.save(slots);
        });

    }

    @Override
    public List<TimeSlot> getAvailableSlotForFollowUpAppointment(Integer veterinarianId, Integer currentSlotId) {

        List<TimeSlot> timeSlots = this.getAvailableSlotsByVeterinarianId(veterinarianId); // Get All available slots

        // Filter only slots come after the slot of main appointment
        List<TimeSlot> filteredSlots = timeSlots.stream()
                .filter(timeSlot -> timeSlot.getSlotId() > currentSlotId)
                .collect(Collectors.toList());

        return filteredSlots;
    }

    @Override
    public List<TimeSlot> getBookedTimeSlots() {
        List<TimeSlot> timeSlots = timeSlotRepository.findBookedTimeSlots();

        if (timeSlots.isEmpty()) { // Not found
            throw new TimeSlotNotFoundException("There are no booked slots");
        }
        return timeSlots;
    }

    @Override   // get all available slots prior to current_time 3 hours
    public List<TimeSlot> getAvailableSlots() {

        LocalDateTime currentDate = LocalDateTime.now();
        LocalDateTime threeHoursFromNow = currentDate.plusHours(3);
        Integer nextThreeHour = threeHoursFromNow.getHour();
        Integer nextSlot = 1;
        if (nextThreeHour <= 10) {
            nextSlot = 2;
        } else if (nextThreeHour <= 13) {
            nextSlot = 3;
        } else if (nextThreeHour <= 15) {
            nextSlot = 4;
        } else {
            currentDate = currentDate.plusDays(1);
        }

        Integer currentYear = currentDate.getYear();
        Integer currentMonth = currentDate.getMonthValue();
        Integer currentDay = currentDate.getDayOfMonth();

        LocalDateTime endDate = currentDate.plusDays(30);
        Integer endYear = endDate.getYear();
        Integer endMonth = endDate.getMonthValue();
        Integer endDay = endDate.getDayOfMonth();

        List<TimeSlot> timeSlots = timeSlotRepository.findAvailableTimeSlot(currentYear, currentMonth, currentDay, nextSlot, endYear, endMonth, endDay);
        if (timeSlots.isEmpty()) {
            throw new TimeSlotNotFoundException("There are no available slots");
        }
        return timeSlots;
    }

    @Override
    public VeterinarianSlots getVeterinarianSlotById(Integer veterinarianId, Integer slotId) {
        VeterinarianSlotId veterinarianSlotId = new VeterinarianSlotId();
        veterinarianSlotId.setSlotId(slotId);
        veterinarianSlotId.setVeterinarianId(veterinarianId);

        VeterinarianSlots veterinarianSlots = veterinarianSlotsRepository.findVeterinarianSlotsByVeterinarianSlotId(veterinarianSlotId);
        if (veterinarianSlots == null)
            throw new TimeSlotNotFoundException("There are no veterinarian slots");
        return veterinarianSlots;
    }

}
