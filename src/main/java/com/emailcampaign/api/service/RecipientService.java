package com.emailcampaign.api.service;

import com.emailcampaign.api.dto.request.AddRecipientsRequest;
import com.emailcampaign.api.dto.request.RecipientItem;
import com.emailcampaign.api.dto.response.RecipientResponse;
import com.emailcampaign.api.entity.Campaign;
import com.emailcampaign.api.entity.Recipient;
import com.emailcampaign.api.exception.ConflictException;
import com.emailcampaign.api.repository.RecipientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecipientService {

    private static final Logger log = LoggerFactory.getLogger(RecipientService.class);

    private final RecipientRepository recipientRepository;
    private final CampaignService campaignService;

    public RecipientService(RecipientRepository recipientRepository, CampaignService campaignService) {
        this.recipientRepository = recipientRepository;
        this.campaignService = campaignService;
    }

    @Transactional
    public List<RecipientResponse> addRecipients(Long campaignId, AddRecipientsRequest request) {
        Campaign campaign = campaignService.getCampaignOrThrow(campaignId);

        // reject dupes inside the payload itself first - the DB constraint alone can't
        // catch two identical emails arriving in the same insert batch
        Set<String> emailsInRequest = new LinkedHashSet<>();
        for (RecipientItem item : request.getRecipients()) {
            String email = item.getEmail().toLowerCase();
            if (!emailsInRequest.add(email)) {
                throw new ConflictException("duplicate email in request: " + item.getEmail());
            }
        }

        List<Recipient> toSave = new ArrayList<>();
        for (RecipientItem item : request.getRecipients()) {
            Recipient recipient = new Recipient();
            recipient.setCampaignId(campaign.getId());
            recipient.setName(item.getName());
            recipient.setEmail(item.getEmail());
            toSave.add(recipient);
        }

        // anything already on this campaign is caught here by the DB's unique
        // (campaign_id, email) constraint - uq_recipient_campaign_email
        List<Recipient> saved;
        try {
            saved = recipientRepository.saveAll(toSave);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("one or more recipients already exist on this campaign");
        }

        log.info("added {} recipients to campaign {}", saved.size(), campaignId);

        return saved.stream().map(RecipientResponse::from).collect(Collectors.toList());
    }
}
