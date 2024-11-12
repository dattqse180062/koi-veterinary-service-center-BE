package org.ftf.koifishveterinaryservicecenter.medicalreport;

import org.assertj.core.api.Assertions;
import org.ftf.koifishveterinaryservicecenter.entity.MedicalReport;
import org.ftf.koifishveterinaryservicecenter.entity.Medicine;
import org.ftf.koifishveterinaryservicecenter.entity.User;
import org.ftf.koifishveterinaryservicecenter.repository.MedicineRepository;
import org.ftf.koifishveterinaryservicecenter.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback
public class MedicineRepositoryTests {

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private UserRepository userRepository;



    @Test
    public void testGetAllMedicineSuccess() {
        List<Medicine> medicines = medicineRepository.findAll();
        Assertions.assertThat(medicines).isNotEmpty();
        medicines.forEach(System.out::println);
    }

    @Test
    public void testCreateMedicalReport() {
        Integer veterinarianId = 11;
        User veterinarian = userRepository.findVeterinarianById(veterinarianId);

        MedicalReport medicalReport = new MedicalReport();
        medicalReport.setVeterinarian(veterinarian);
        medicalReport.setAdvise("Install heaters for temperature control.");
        medicalReport.setConclusion("Pond temperature fluctuation noted.");


    }

}
