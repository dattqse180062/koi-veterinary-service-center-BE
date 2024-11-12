package org.ftf.koifishveterinaryservicecenter.feedbackservice;

import org.assertj.core.api.Assertions;
import org.ftf.koifishveterinaryservicecenter.entity.Feedback;
import org.ftf.koifishveterinaryservicecenter.repository.FeedbackRepository;
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
public class FeedbackRepositoryTests {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Test
    public void testGetAllFeedbacksSuccess() {
        List<Feedback> feedbacks = feedbackRepository.findFeedbackByRating();
        Assertions.assertThat(feedbacks).isNotNull();
        feedbacks.forEach(System.out::println);
    }

}
