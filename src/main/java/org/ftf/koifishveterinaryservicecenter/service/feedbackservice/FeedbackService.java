package org.ftf.koifishveterinaryservicecenter.service.feedbackservice;

import org.ftf.koifishveterinaryservicecenter.entity.Feedback;

import java.util.List;

public interface FeedbackService {
    List<Feedback> getFeedbacksByRating();

    List<Feedback> getAllFeedbacks();

    List<Feedback> getFeedbacksByVeterianrianId(Integer veterianrianId);

    Feedback getFeedbackById(Integer feedbackId);
}
