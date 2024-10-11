package org.ftf.koifishveterinaryservicecenter.repository;

import org.ftf.koifishveterinaryservicecenter.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Integer> {

    @Query("SELECT c FROM Certificate c WHERE c.veterinarian.userId = :veterinarianId")
    List<Certificate> findByVeterinarianId(Integer veterinarianId);
}
