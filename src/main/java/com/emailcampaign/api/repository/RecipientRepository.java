package com.emailcampaign.api.repository;

import com.emailcampaign.api.entity.Recipient;
import com.emailcampaign.api.entity.RecipientStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipientRepository extends JpaRepository<Recipient, Long> {

    List<Recipient> findByCampaignId(Long campaignId);

    List<Recipient> findByCampaignIdAndStatus(Long campaignId, RecipientStatus status);

    long countByCampaignId(Long campaignId);

    long countByCampaignIdAndStatus(Long campaignId, RecipientStatus status);
}
