package com.backend.ai.api;

import com.backend.ai.api.dto.ChatRequest;
import com.backend.ai.api.dto.ChatResponse;
import com.backend.ai.api.dto.ConversationResponse;
import com.backend.ai.api.dto.AdminCampaignAnalysisRequest;
import com.backend.ai.api.dto.AdminCampaignAnalysisResponse;
import com.backend.ai.api.dto.AdminProductPrRequest;
import com.backend.ai.api.dto.AdminProductPrResponse;
import com.backend.ai.api.dto.DrugDraftRequest;
import com.backend.ai.api.dto.DrugDraftResponse;
import com.backend.ai.service.AdminCampaignAnalysisService;
import com.backend.ai.service.AdminDrugDraftService;
import com.backend.ai.service.AdminProductPrService;
import com.backend.ai.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final AdminDrugDraftService adminDrugDraftService;
    private final AdminCampaignAnalysisService adminCampaignAnalysisService;
    private final AdminProductPrService adminProductPrService;

    public ChatController(
            ChatService chatService,
            AdminDrugDraftService adminDrugDraftService,
            AdminCampaignAnalysisService adminCampaignAnalysisService,
            AdminProductPrService adminProductPrService) {
        this.chatService = chatService;
        this.adminDrugDraftService = adminDrugDraftService;
        this.adminCampaignAnalysisService = adminCampaignAnalysisService;
        this.adminProductPrService = adminProductPrService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(chatService.health());
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(chatService.chat(request, jwt));
    }

    @PostMapping("/admin/drug-draft")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DrugDraftResponse> generateDrugDraft(
            @Valid @RequestBody DrugDraftRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(adminDrugDraftService.generateDraft(request));
    }

    @PostMapping("/admin/campaign-analysis")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminCampaignAnalysisResponse> analyzeCampaign(
            @Valid @RequestBody AdminCampaignAnalysisRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(adminCampaignAnalysisService.analyze(request));
    }

    @PostMapping("/admin/product-pr")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminProductPrResponse> generateProductPr(
            @Valid @RequestBody AdminProductPrRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(adminProductPrService.generate(request));
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ConversationResponse> conversation(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(chatService.getConversation(conversationId, jwt));
    }
}
