package com.emailcampaign.api.controller;

import com.emailcampaign.api.dto.request.AddRecipientsRequest;
import com.emailcampaign.api.dto.request.CreateCampaignRequest;
import com.emailcampaign.api.dto.response.*;
import com.emailcampaign.api.entity.CampaignStatus;
import com.emailcampaign.api.service.CampaignProcessingService;
import com.emailcampaign.api.service.CampaignService;
import com.emailcampaign.api.service.RecipientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
@Tag(name = "Campaigns", description = "Create campaigns, manage recipients, schedule and track sends")
public class CampaignController {

    private final CampaignService campaignService;
    private final RecipientService recipientService;
    private final CampaignProcessingService campaignProcessingService;

    public CampaignController(CampaignService campaignService,
                               RecipientService recipientService,
                               CampaignProcessingService campaignProcessingService) {
        this.campaignService = campaignService;
        this.recipientService = recipientService;
        this.campaignProcessingService = campaignProcessingService;
    }

    @PostMapping
    @Operation(summary = "Create a new campaign in DRAFT status")
    public ResponseEntity<CampaignResponse> create(@Valid @RequestBody CreateCampaignRequest request) {
        CampaignResponse response = campaignService.createCampaign(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{campaignId}/recipients")
    @Operation(summary = "Add one or more recipients to a draft campaign")
    public ResponseEntity<List<RecipientResponse>> addRecipients(@PathVariable Long campaignId,
                                                                   @Valid @RequestBody AddRecipientsRequest request) {
        List<RecipientResponse> response = recipientService.addRecipients(campaignId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{campaignId}/schedule")
    @Operation(summary = "Schedule a draft campaign for sending")
    public ResponseEntity<CampaignResponse> schedule(@PathVariable Long campaignId) {
        return ResponseEntity.ok(campaignService.scheduleCampaign(campaignId));
    }

    @PostMapping("/process")
    @Operation(summary = "Manually trigger processing of every due, scheduled campaign " +
            "(this also happens automatically in the background)")
    public ResponseEntity<ProcessResultResponse> process() {
        int processed = campaignProcessingService.processDueCampaigns();
        return ResponseEntity.ok(new ProcessResultResponse(processed, processed + " campaign(s) processed"));
    }

    @GetMapping
    @Operation(summary = "List campaigns with pagination, status filter, name search and created-date sorting")
    public ResponseEntity<PagedResponse<CampaignResponse>> list(
            @Parameter(description = "filter by exact status, e.g. DRAFT, SCHEDULED, PROCESSING, COMPLETED")
            @RequestParam(required = false) CampaignStatus status,
            @Parameter(description = "case-insensitive partial match on campaign name")
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "asc or desc, sorted by createdAt")
            @RequestParam(defaultValue = "desc") String sort) {

        Sort.Direction direction = "asc".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));

        return ResponseEntity.ok(campaignService.listCampaigns(status, search, pageable));
    }

    @GetMapping("/{campaignId}")
    @Operation(summary = "Get a single campaign's details")
    public ResponseEntity<CampaignResponse> get(@PathVariable Long campaignId) {
        return ResponseEntity.ok(campaignService.getCampaign(campaignId));
    }

    @GetMapping("/{campaignId}/statistics")
    @Operation(summary = "Get delivery statistics for a campaign")
    public ResponseEntity<CampaignStatisticsResponse> statistics(@PathVariable Long campaignId) {
        return ResponseEntity.ok(campaignService.getStatistics(campaignId));
    }
}
