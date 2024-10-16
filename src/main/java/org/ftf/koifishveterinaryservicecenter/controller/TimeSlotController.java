package org.ftf.koifishveterinaryservicecenter.controller;

import org.ftf.koifishveterinaryservicecenter.dto.TimeSlotDto;
import org.ftf.koifishveterinaryservicecenter.entity.TimeSlot;
import org.ftf.koifishveterinaryservicecenter.exception.TimeSlotNotFound;
import org.ftf.koifishveterinaryservicecenter.exception.UserNotFoundException;
import org.ftf.koifishveterinaryservicecenter.mapper.TimeSlotMapper;
import org.ftf.koifishveterinaryservicecenter.service.slotservice.SlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
//@CrossOrigin
@RequestMapping("/api/v1/slots")
public class TimeSlotController {
    private final SlotService slotService;

    @Autowired
    public TimeSlotController(final SlotService slotService) {
        this.slotService = slotService;
    }

    @GetMapping("/{veterinarianID}")
    public ResponseEntity<?> getVeterinarianSlots(@PathVariable("veterinarianID") final Integer veterinarianID) {
        try {
            List<TimeSlot> slots = slotService.getVeterinarianSlots(veterinarianID);
            List<TimeSlotDto> dtos = slots.stream()
                    .map(TimeSlotMapper.INSTANCE::convertToTimeSlotDto)
                    .collect(Collectors.toList());
            return new ResponseEntity<>(dtos, HttpStatus.OK);
//            return new ResponseEntity<>(slots, HttpStatus.OK);
        } catch (UserNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (TimeSlotNotFound e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    /*
     * Get all available slots since current day for customer for choosing in case not specifying veterinarian
     * Actors: Customer
     * */
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableSlots() {
        try {
            List<TimeSlot> slots = slotService.getAvailableSlots();
            List<TimeSlotDto> slotDtos = slots.stream()
                    .map(TimeSlotMapper.INSTANCE::convertToAvailableTimeSlotDto)
                    .collect(Collectors.toList());
            return new ResponseEntity<>(slotDtos, HttpStatus.OK);
        } catch (TimeSlotNotFound e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /*
     * Get all available slots since current day for customer for choosing in case not specifying veterinarian
     * Actors: Customer
     * */
    @GetMapping("/{veterinarianId}/available")
    public ResponseEntity<?> getAvailableSlotsByVeterinarianId(
            @PathVariable("veterinarianId") final Integer veterinarianId) {
        try {
            List<TimeSlot> slots = slotService.getAvailableSlotsByVeterinarianId(veterinarianId);
            List<TimeSlotDto> slotDtos = slots.stream()
                    .map(TimeSlotMapper.INSTANCE::convertToAvailableTimeSlotDto)
                    .collect(Collectors.toList());
            return new ResponseEntity<>(slotDtos, HttpStatus.OK);
        } catch (TimeSlotNotFound e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NO_CONTENT);
        } catch (UserNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
