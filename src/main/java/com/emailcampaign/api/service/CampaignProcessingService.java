package com.emailcampaign.api.service;

import com.emailcampaign.api.entity.Campaign;
import com.emailcampaign.api.entity.CampaignStatus;
import com.emailcampaign.api.entity.Recipient;
import com.emailcampaign.api.entity.RecipientStatus;
import com.emailcampaign.api.kafka.CampaignEventPublisher;
import com.emailcampaign.api.kafka.CampaignProcessedEvent;
import com.emailcampaign.api.repository.CampaignRepository;
import com.emailcampaign.api.repository.RecipientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// every recipient just gets randomly flipped to DELIVERED or FAILED.
@Service
public class CampaignProcessingService {

    private static final Logger log = LoggerFactory.getLogger(CampaignProcessingService.class);

    private final CampaignRepository campaignRepository;
    private final RecipientRepository recipientRepository;
    private final CampaignEventPublisher campaignEventPublisher;

    public CampaignProcessingService(CampaignRepository campaignRepository,
                                      RecipientRepository recipientRepository,
                                      CampaignEventPublisher campaignEventPublisher) {
        this.campaignRepository = campaignRepository;
        this.recipientRepository = recipientRepository;
        this.campaignEventPublisher = campaignEventPublisher;
    }

    // called from POST /api/campaigns/process
    public int processDueCampaigns() {
        List<Campaign> due = campaignRepository.findByStatusAndScheduledAtLessThanEqual(
                CampaignStatus.SCHEDULED, OffsetDateTime.now());

        int processedCount = 0;
        for (Campaign campaign : due) {
            if (processCampaign(campaign.getId())) {
                processedCount++;
            }
        }
        return processedCount;
    }

    @Transactional
    public boolean processCampaign(Long campaignId) {

        // Atomic conditional update: only flips status if it's still SCHEDULED.
        // This is what makes processing safe even if this method runs concurrently.
        int updated = campaignRepository.markProcessingIfScheduled(campaignId);
        if (updated == 0) {
            log.debug("campaign {} wasn't SCHEDULED, skipping", campaignId);
            return false;
        }

        List<Recipient> pending = recipientRepository.findByCampaignIdAndStatus(campaignId, RecipientStatus.PENDING);
        for (Recipient recipient : pending) {
            boolean delivered = ThreadLocalRandom.current().nextBoolean();
            recipient.setStatus(delivered ? RecipientStatus.DELIVERED : RecipientStatus.FAILED);
        }
        recipientRepository.saveAll(pending);

        Campaign campaign = campaignRepository.findById(campaignId).orElseThrow();
        campaign.setStatus(CampaignStatus.COMPLETED);
        campaignRepository.save(campaign);

        log.info("processed campaign {}: {} recipients", campaignId, pending.size());
        campaignEventPublisher.publishCampaignProcessed(
                new CampaignProcessedEvent(campaignId, pending.size(), CampaignStatus.COMPLETED.name()));
        return true;
    }
}