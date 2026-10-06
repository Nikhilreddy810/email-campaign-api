package com.emailcampaign.api.service;

import com.emailcampaign.api.dto.request.CreateCampaignRequest;
import com.emailcampaign.api.dto.response.CampaignResponse;
import com.emailcampaign.api.dto.response.CampaignStatisticsResponse;
import com.emailcampaign.api.dto.response.PagedResponse;
import com.emailcampaign.api.entity.Campaign;
import com.emailcampaign.api.entity.CampaignStatus;
import com.emailcampaign.api.entity.RecipientStatus;
import com.emailcampaign.api.exception.BadRequestException;
import com.emailcampaign.api.exception.ResourceNotFoundException;
import com.emailcampaign.api.repository.CampaignRepository;
import com.emailcampaign.api.repository.RecipientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class CampaignService {

    private static final Logger log = LoggerFactory.getLogger(CampaignService.class);

    private final CampaignRepository campaignRepository;
    private final RecipientRepository recipientRepository;

    public CampaignService(CampaignRepository campaignRepository, RecipientRepository recipientRepository) {
        this.campaignRepository = campaignRepository;
        this.recipientRepository = recipientRepository;
    }

    @Transactional
    public CampaignResponse createCampaign(CreateCampaignRequest request) {
        Campaign campaign = new Campaign();
        campaign.setName(request.getName());
        campaign.setSubject(request.getSubject());
        campaign.setSenderEmail(request.getSenderEmail());
        campaign.setContent(request.getContent());
        campaign.setScheduledAt(request.getScheduledAt());
        campaign.setStatus(CampaignStatus.DRAFT);

        Campaign saved = campaignRepository.save(campaign);
        log.info("Created campaign id={} name='{}'", saved.getId(), saved.getName());
        return CampaignResponse.from(saved);
    }

    public Campaign getCampaignOrThrow(Long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("campaign not found: " + id));
    }

    public CampaignResponse getCampaign(Long id) {
        return CampaignResponse.from(getCampaignOrThrow(id));
    }

    public PagedResponse<CampaignResponse> listCampaigns(CampaignStatus status, String search, Pageable pageable) {
        Page<Campaign> page = campaignRepository.search(status, search, pageable);
        return PagedResponse.from(page.map(CampaignResponse::from));
    }

    @Transactional
    public CampaignResponse scheduleCampaign(Long id) {
        Campaign campaign = getCampaignOrThrow(id);

        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new BadRequestException("only a draft campaign can be scheduled, current status is " + campaign.getStatus());
        }

        long recipientCount = recipientRepository.countByCampaignId(id);
        if (recipientCount == 0) {
            throw new BadRequestException("campaign must have at least one recipient before it can be scheduled");
        }

        if (!campaign.getScheduledAt().isAfter(OffsetDateTime.now())) {
            throw new BadRequestException("scheduled time must be in the future");
        }

        campaign.setStatus(CampaignStatus.SCHEDULED);
        Campaign saved = campaignRepository.save(campaign);
        log.info("Scheduled campaign id={} for {}", saved.getId(), saved.getScheduledAt());
        return CampaignResponse.from(saved);
    }

    public CampaignStatisticsResponse getStatistics(Long id) {
        Campaign campaign = getCampaignOrThrow(id);

        long total = recipientRepository.countByCampaignId(id);
        long delivered = recipientRepository.countByCampaignIdAndStatus(id, RecipientStatus.DELIVERED);
        long failed = recipientRepository.countByCampaignIdAndStatus(id, RecipientStatus.FAILED);
        long pending = recipientRepository.countByCampaignIdAndStatus(id, RecipientStatus.PENDING);

        return new CampaignStatisticsResponse(CampaignResponse.from(campaign), total, delivered, failed, pending);
    }
}
