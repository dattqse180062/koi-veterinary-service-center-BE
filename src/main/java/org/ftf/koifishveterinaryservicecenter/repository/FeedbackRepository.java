package org.ftf.koifishveterinaryservicecenter.repository;

import org.ftf.koifishveterinaryservicecenter.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
  }