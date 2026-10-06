package com.emailcampaign.api.service;

import com.emailcampaign.api.entity.Campaign;
import com.emailcampaign.api.entity.CampaignStatus;
import com.emailcampaign.api.entity.Recipient;
import com.emailcampaign.api.entity.RecipientStatus;
import com.emailcampaign.api.kafka.CampaignEventPublisher;
import com.emailcampaign.api.repository.CampaignRepository;
import com.emailcampaign.api.repository.RecipientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class CampaignProcessingServiceTest {

    private CampaignRepository campaignRepository;
    private RecipientRepository recipientRepository;
    private CampaignEventPublisher campaignEventPublisher;
    private CampaignProcessingService processingService;

    @BeforeEach
    void setUp() {
        campaignRepository = mock(CampaignRepository.class);
        recipientRepository = mock(RecipientRepository.class);
        campaignEventPublisher = mock(CampaignEventPublisher.class);
        processingService = new CampaignProcessingService(campaignRepository, recipientRepository, campaignEventPublisher);
    }

    @Test
    void processCampaign_marksRecipientsAndCompletesCampaign() {
        // markProcessingIfScheduled returns 1 row updated, meaning we "won" the race
        when(campaignRepository.markProcessingIfScheduled(1L)).thenReturn(1);

        Recipient r1 = new Recipient();
        r1.setStatus(RecipientStatus.PENDING);
        Recipient r2 = new Recipient();
        r2.setStatus(RecipientStatus.PENDING);
        when(recipientRepository.findByCampaignIdAndStatus(1L, RecipientStatus.PENDING))
                .thenReturn(List.of(r1, r2));

        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setStatus(CampaignStatus.PROCESSING);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        boolean result = processingService.processCampaign(1L);

        assertThat(result).isTrue();
        assertThat(r1.getStatus()).isIn(RecipientStatus.DELIVERED, RecipientStatus.FAILED);
        assertThat(r2.getStatus()).isIn(RecipientStatus.DELIVERED, RecipientStatus.FAILED);
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.COMPLETED);
        verify(recipientRepository).saveAll(List.of(r1, r2));
        verify(campaignRepository).save(campaign);
    }

    @Test
    void processCampaign_doesNothingWhenAlreadyProcessed() {
        // 0 rows updated means the campaign was not SCHEDULED anymore (already handled)
        when(campaignRepository.markProcessingIfScheduled(2L)).thenReturn(0);

        boolean result = processingService.processCampaign(2L);

        assertThat(result).isFalse();
        verify(recipientRepository, never()).findByCampaignIdAndStatus(anyLong(), any());
        verify(campaignRepository, never()).save(any(Campaign.class));
    }

    @Test
    void processCampaign_calledTwice_secondCallIsNoOp() {
        // first call claims it
        when(campaignRepository.markProcessingIfScheduled(3L))
                .thenReturn(1)   // first call succeeds
                .thenReturn(0);  // second call finds it's no longer SCHEDULED

        when(recipientRepository.findByCampaignIdAndStatus(3L, RecipientStatus.PENDING))
                .thenReturn(List.of());

        Campaign campaign = new Campaign();
        campaign.setId(3L);
        when(campaignRepository.findById(3L)).thenReturn(Optional.of(campaign));

        boolean first = processingService.processCampaign(3L);
        boolean second = processingService.processCampaign(3L);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        verify(campaignRepository, times(1)).save(any(Campaign.class));
    }
}
