package com.emailcampaign.api.repository;

import com.emailcampaign.api.entity.Campaign;
import com.emailcampaign.api.entity.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    // status/name are both optional filters, pass null to skip either one
    @Query("""
            SELECT c FROM Campaign c
            WHERE (:status IS NULL OR c.status = :status)
            AND (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))
            """)
    Page<Campaign> search(@Param("status") CampaignStatus status,
                           @Param("name") String name,
                           Pageable pageable);

    List<Campaign> findByStatusAndScheduledAtLessThanEqual(CampaignStatus status, OffsetDateTime now);

    @Modifying
    @Query("UPDATE Campaign c SET c.status = :processingStatus, c.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE c.id = :id AND c.status = :scheduledStatus")
    int markProcessingIfScheduled(@Param("id") Long id,
                                   @Param("processingStatus") CampaignStatus processingStatus,
                                   @Param("scheduledStatus") CampaignStatus scheduledStatus);

    // wraps the query above so callers don't have to pass the enum constants every time
    default int markProcessingIfScheduled(Long id) {
        return markProcessingIfScheduled(id, CampaignStatus.PROCESSING, CampaignStatus.SCHEDULED);
    }
}