package com.backend.content.service;

import com.backend.content.api.dto.ReportCreateRequest;
import com.backend.content.model.Report;
import com.backend.content.model.ReportReason;
import com.backend.content.model.ReportStatus;
import com.backend.content.model.TargetType;
import com.backend.content.repo.AnswerRepository;
import com.backend.content.repo.PostRepository;
import com.backend.content.repo.ReportRepository;
import com.backend.content.repo.ThreadRepository;
import com.backend.content.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final ThreadRepository threadRepository;
    private final AnswerRepository answerRepository;

    public ReportService(ReportRepository reportRepository,
            PostRepository postRepository,
            ThreadRepository threadRepository,
            AnswerRepository answerRepository) {
        this.reportRepository = reportRepository;
        this.postRepository = postRepository;
        this.threadRepository = threadRepository;
        this.answerRepository = answerRepository;
    }

    public Report create(ReportCreateRequest req) {
        UUID actorId = SecurityUtils.getActorId();
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        TargetType targetType = TargetType.valueOf(req.targetType().trim().toUpperCase());
        ensureTargetExists(targetType, req.targetId());

        Report report = new Report();
        report.setId(UUID.randomUUID());
        report.setTargetType(targetType);
        report.setTargetId(req.targetId());
        report.setReporterId(actorId);
        report.setReason(ReportReason.valueOf(req.reason().trim().toUpperCase()));
        report.setNote(req.note());
        report.setStatus(ReportStatus.OPEN);
        report.setCreatedAt(Instant.now());
        return reportRepository.save(report);
    }

    private void ensureTargetExists(TargetType targetType, UUID targetId) {
        switch (targetType) {
            case POST -> postRepository.findById(targetId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
            case THREAD -> threadRepository.findById(targetId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));
            case ANSWER -> answerRepository.findById(targetId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Answer not found"));
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid target type");
        }
    }
}
