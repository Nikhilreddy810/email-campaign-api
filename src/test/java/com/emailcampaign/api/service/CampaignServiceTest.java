package com.emailcampaign.api.service;

import com.emailcampaign.api.dto.request.CreateCampaignRequest;
import com.emailcampaign.api.dto.response.CampaignResponse;
import com.emailcampaign.api.dto.response.CampaignStatisticsResponse;
import com.emailcampaign.api.entity.Campaign;
import com.emailcampaign.api.entity.CampaignStatus;
import com.emailcampaign.api.entity.RecipientStatus;
import com.emailcampaign.api.exception.BadRequestException;
import com.emailcampaign.api.exception.ResourceNotFoundException;
import com.emailcampaign.api.repository.CampaignRepository;
import com.emailcampaign.api.repository.RecipientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CampaignServiceTest {

    private CampaignRepository campaignRepository;
    private RecipientRepository recipientRepository;
    private CampaignService campaignService;

    @BeforeEach
    void setUp() {
        campaignRepository = mock(CampaignRepository.class);
        recipientRepository = mock(RecipientRepository.class);
        campaignService = new CampaignService(campaignRepository, recipientRepository);
    }

    @Test
    void createCampaign_savesAsDraft() {
        CreateCampaignRequest request = new CreateCampaignRequest();
        request.setName("Summer Sale");
        request.setSubject("50% off everything");
        request.setSenderEmail("promo@shop.com");
        request.setContent("Big sale this weekend");
        request.setScheduledAt(OffsetDateTime.now().plusDays(1));

        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> {
            Campaign c = inv.getArgument(0);
            c.setId(1L);
            c.setCreatedAt(OffsetDateTime.now());
            c.setUpdatedAt(OffsetDateTime.now());
            return c;
        });

        CampaignResponse response = campaignService.createCampaign(request);

        ArgumentCaptor<Campaign> captor = ArgumentCaptor.forClass(Campaign.class);
        verify(campaignRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(response.getName()).isEqualTo("Summer Sale");
        assertThat(response.getStatus()).isEqualTo("DRAFT");
    }

    @Test
    void scheduleCampaign_failsWithoutRecipients() {
        Campaign campaign = draftCampaign();
        when(campaignRepository.findById(1L)).thenReturn(java.util.Optional.of(campaign));
        when(recipientRepository.countByCampaignId(1L)).thenReturn(0L);

        assertThatThrownBy(() -> campaignService.scheduleCampaign(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least one recipient");

        verify(campaignRepository, never()).save(any());
    }

    @Test
    void scheduleCampaign_failsWhenNotDraft() {
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.SCHEDULED);
        when(campaignRepository.findById(1L)).thenReturn(java.util.Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.scheduleCampaign(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only a draft campaign");
    }

    @Test
    void scheduleCampaign_succeedsWithRecipientsAndFutureDate() {
        Campaign campaign = draftCampaign();
        when(campaignRepository.findById(1L)).thenReturn(java.util.Optional.of(campaign));
        when(recipientRepository.countByCampaignId(1L)).thenReturn(3L);
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));

        CampaignResponse response = campaignService.scheduleCampaign(1L);

        assertThat(response.getStatus()).isEqualTo("SCHEDULED");
    }

    @Test
    void getCampaign_throwsWhenMissing() {
        when(campaignRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> campaignService.getCampaign(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getStatistics_returnsCorrectCounts() {
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.COMPLETED);
        when(campaignRepository.findById(1L)).thenReturn(java.util.Optional.of(campaign));
        when(recipientRepository.countByCampaignId(1L)).thenReturn(10L);
        when(recipientRepository.countByCampaignIdAndStatus(1L, RecipientStatus.DELIVERED)).thenReturn(6L);
        when(recipientRepository.countByCampaignIdAndStatus(1L, RecipientStatus.FAILED)).thenReturn(3L);
        when(recipientRepository.countByCampaignIdAndStatus(1L, RecipientStatus.PENDING)).thenReturn(1L);

        CampaignStatisticsResponse stats = campaignService.getStatistics(1L);

        assertThat(stats.getTotalRecipients()).isEqualTo(10L);
        assertThat(stats.getDelivered()).isEqualTo(6L);
        assertThat(stats.getFailed()).isEqualTo(3L);
        assertThat(stats.getPending()).isEqualTo(1L);
    }

    private Campaign draftCampaign() {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setName("Test Campaign");
        campaign.setSubject("Subject");
        campaign.setSenderEmail("sender@test.com");
        campaign.setContent("content");
        campaign.setScheduledAt(OffsetDateTime.now().plusDays(1));
        campaign.setStatus(CampaignStatus.DRAFT);
        campaign.setCreatedAt(OffsetDateTime.now());
        campaign.setUpdatedAt(OffsetDateTime.now());
        return campaign;
    }
}
