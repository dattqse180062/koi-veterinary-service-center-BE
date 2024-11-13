package org.ftf.koifishveterinaryservicecenter.repository;

import org.ftf.koifishveterinaryservicecenter.entity.veterinarian_slots.VeterinarianSlotId;
import org.ftf.koifishveterinaryservicecenter.entity.veterinarian_slots.VeterinarianSlots;
import org.ftf.koifishveterinaryservicecenter.enums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.swing.text.html.Option;
import java.util.List;

public interface VeterinarianSlotsRepository extends JpaRepository<VeterinarianSlots, VeterinarianSlotId> {
    @Query("SELECT vs FROM VeterinarianSlots vs WHERE vs.veterinarian.userId = :veterinarianId")
    List<VeterinarianSlots> findByVeterinarianId(@Param("veterinarianId") Integer veterinarianId);

    @Query("SELECT vs FROM VeterinarianSlots vs WHERE vs.status = ?1 AND vs.veterinarianSlotId.slotId = ?2")
    List<VeterinarianSlots> getAvailableSlotsBySlotId(SlotStatus status, Integer slotId);

    @Query("SELECT vs FROM VeterinarianSlots vs WHERE vs.veterinarianSlotId.veterinarianId = ?1 AND vs.veterinarianSlotId.slotId = ?2")
    VeterinarianSlots getVeterinarianSlotsById(Integer veterinarianId, Integer slotId);

    @Query("SELECT COUNT(vs) FROM VeterinarianSlots vs " +
            "WHERE vs.veterinarianSlotId.veterinarianId = :vetId " +
            "AND vs.status = 'BOOKED' " +
            "AND vs.timeSlot.year = :year " +
            "AND vs.timeSlot.month = :month " +
            "AND vs.timeSlot.day = :day")
    long countSlotsByVetInDate(@Param("vetId") Integer vetId,
                               @Param("year") Integer year,
                               @Param("month") Integer month,
                               @Param("day") Integer day);

    @Query("SELECT vs FROM  VeterinarianSlots vs WHERE vs.veterinarianSlotId = ?1")
    VeterinarianSlots findVeterinarianSlotsByVeterinarianSlotId(VeterinarianSlotId veterinarianSlotId);

}