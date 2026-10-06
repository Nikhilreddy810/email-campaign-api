package com.emailcampaign.api.service;

import com.emailcampaign.api.dto.request.AddRecipientsRequest;
import com.emailcampaign.api.dto.request.RecipientItem;
import com.emailcampaign.api.dto.response.RecipientResponse;
import com.emailcampaign.api.entity.Campaign;
import com.emailcampaign.api.entity.CampaignStatus;
import com.emailcampaign.api.entity.Recipient;
import com.emailcampaign.api.exception.ConflictException;
import com.emailcampaign.api.repository.RecipientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecipientServiceTest {

    private RecipientRepository recipientRepository;
    private CampaignService campaignService;
    private RecipientService recipientService;

    @BeforeEach
    void setUp() {
        recipientRepository = mock(RecipientRepository.class);
        campaignService = mock(CampaignService.class);
        recipientService = new RecipientService(recipientRepository, campaignService);
    }

    @Test
    void addRecipients_savesAllWhenNoDuplicates() {
        Campaign campaign = draftCampaign();
        when(campaignService.getCampaignOrThrow(1L)).thenReturn(campaign);
        when(recipientRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<Recipient> input = inv.getArgument(0);
            for (int i = 0; i < input.size(); i++) {
                input.get(i).setId((long) (i + 1));
            }
            return input;
        });

        AddRecipientsRequest request = new AddRecipientsRequest();
        request.setRecipients(List.of(item("Alice", "alice@test.com"), item("Bob", "bob@test.com")));

        List<RecipientResponse> result = recipientService.addRecipients(1L, request);

        assertThat(result).hasSize(2);
        verify(recipientRepository).saveAll(anyList());
    }

    @Test
    void addRecipients_rejectsDuplicateWithinSameRequest() {
        Campaign campaign = draftCampaign();
        when(campaignService.getCampaignOrThrow(1L)).thenReturn(campaign);

        AddRecipientsRequest request = new AddRecipientsRequest();
        request.setRecipients(List.of(item("Alice", "alice@test.com"), item("Alice again", "ALICE@test.com")));

        assertThatThrownBy(() -> recipientService.addRecipients(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("duplicate email in request");

        verify(recipientRepository, never()).saveAll(anyList());
    }

    @Test
    void addRecipients_rejectsEmailAlreadyOnCampaign() {
        Campaign campaign = draftCampaign();
        when(campaignService.getCampaignOrThrow(1L)).thenReturn(campaign);
        // DB's unique (campaign_id, email) constraint is what actually catches this
        when(recipientRepository.saveAll(anyList())).thenThrow(new DataIntegrityViolationException("dup"));

        AddRecipientsRequest request = new AddRecipientsRequest();
        request.setRecipients(List.of(item("Alice", "alice@test.com")));

        assertThatThrownBy(() -> recipientService.addRecipients(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exist");
    }

    private RecipientItem item(String name, String email) {
        RecipientItem item = new RecipientItem();
        item.setName(name);
        item.setEmail(email);
        return item;
    }

    private Campaign draftCampaign() {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setStatus(CampaignStatus.DRAFT);
        return campaign;
    }
}
