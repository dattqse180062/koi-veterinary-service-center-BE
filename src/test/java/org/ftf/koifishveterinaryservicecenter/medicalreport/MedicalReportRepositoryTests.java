package org.ftf.koifishveterinaryservicecenter.medicalreport;

import org.assertj.core.api.Assertions;
import org.ftf.koifishveterinaryservicecenter.entity.MedicalReport;
import org.ftf.koifishveterinaryservicecenter.entity.User;
import org.ftf.koifishveterinaryservicecenter.repository.MedicalReportRepository;
import org.ftf.koifishveterinaryservicecenter.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;


@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = true)
public class MedicalReportRepositoryTests {


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MedicalReportRepository medicalReportRepository;


    @Test
    public void testCreateMedicalReport() {
        Integer veterinarianId = 13;
        User veterinarian = userRepository.findVeterinarianById(veterinarianId);

        MedicalReport medicalReport = new MedicalReport();
        medicalReport.setVeterinarian(veterinarian);
        medicalReport.setAdvise("Install heaters for temperature control.");
        medicalReport.setConclusion("Pond temperature fluctuation noted.");

        MedicalReport savedMedicalReport = medicalReportRepository.save(medicalReport);

        Assertions.assertThat(savedMedicalReport.getVeterinarian()).isEqualTo(veterinarian);
        Assertions.assertThat(savedMedicalReport.getPrescription()).isNull();
    }
}
