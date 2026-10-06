package com.emailcampaign.api.validation;

import com.emailcampaign.api.dto.request.CreateCampaignRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateCampaignRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void validRequest_hasNoViolations() {
        CreateCampaignRequest request = validRequest();
        Set<ConstraintViolation<CreateCampaignRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void blankName_isRejected() {
        CreateCampaignRequest request = validRequest();
        request.setName(" ");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void invalidSenderEmail_isRejected() {
        CreateCampaignRequest request = validRequest();
        request.setSenderEmail("not-an-email");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void scheduledAtInThePast_isRejected() {
        CreateCampaignRequest request = validRequest();
        request.setScheduledAt(OffsetDateTime.now().minusDays(1));
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void missingScheduledAt_isRejected() {
        CreateCampaignRequest request = validRequest();
        request.setScheduledAt(null);
        assertThat(validator.validate(request)).isNotEmpty();
    }

    private CreateCampaignRequest validRequest() {
        CreateCampaignRequest request = new CreateCampaignRequest();
        request.setName("Launch Announcement");
        request.setSubject("We're live!");
        request.setSenderEmail("hello@company.com");
        request.setContent("Check out our new product.");
        request.setScheduledAt(OffsetDateTime.now().plusDays(2));
        return request;
    }
}
