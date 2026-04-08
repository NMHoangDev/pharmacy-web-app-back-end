package com.backend.content.service;

import com.backend.content.api.dto.ModerationQueueItem;
import com.backend.content.model.Answer;
import com.backend.content.model.ModerationLog;
import com.backend.content.model.ModerationStatus;
import com.backend.content.model.Post;
import com.backend.content.model.TargetType;
import com.backend.content.model.Thread;
import com.backend.content.repo.AnswerRepository;
import com.backend.content.repo.ModerationLogRepository;
import com.backend.content.repo.PostRepository;
import com.backend.content.repo.ThreadRepository;
import com.backend.content.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ModerationService {

    private final PostRepository postRepository;
    private final ThreadRepository threadRepository;
    private final AnswerRepository answerRepository;
    private final ModerationLogRepository logRepository;

    public ModerationService(PostRepository postRepository,
            ThreadRepository threadRepository,
            AnswerRepository answerRepository,
            ModerationLogRepository logRepository) {
        this.postRepository = postRepository;
        this.threadRepository = threadRepository;
        this.answerRepository = answerRepository;
        this.logRepository = logRepository;
    }

    public List<ModerationQueueItem> queue() {
        List<ModerationQueueItem> items = new ArrayList<>();
        postRepository.findTop100ByModerationStatusOrderByCreatedAtDesc(ModerationStatus.PENDING)
                .forEach(p -> items.add(new ModerationQueueItem(
                        "POST", p.getId(), p.getTitle(), p.getExcerpt(), p.getCreatedAt(),
                        p.getModerationStatus().name())));

        threadRepository.findTop100ByModerationStatusOrderByCreatedAtDesc(ModerationStatus.PENDING)
                .forEach(t -> items.add(new ModerationQueueItem(
                        "THREAD", t.getId(), t.getTitle(), t.getContent(), t.getCreatedAt(),
                        t.getModerationStatus().name())));

        answerRepository.findTop100ByModerationStatusOrderByCreatedAtDesc(ModerationStatus.PENDING)
                .forEach(a -> items.add(new ModerationQueueItem(
                        "ANSWER", a.getId(), null, a.getContent(), a.getCreatedAt(),
                        a.getModerationStatus().name())));

        return items;
    }

    public void approve(TargetType type, UUID id) {
        updateStatus(type, id, ModerationStatus.PUBLISHED, "APPROVE", null);
    }

    public void reject(TargetType type, UUID id, String reason) {
        updateStatus(type, id, ModerationStatus.REJECTED, "REJECT", reason);
    }

    public void hide(TargetType type, UUID id, String reason) {
        updateStatus(type, id, ModerationStatus.HIDDEN, "HIDE", reason);
    }

    private void updateStatus(TargetType type, UUID id, ModerationStatus status, String action, String reason) {
        switch (type) {
            case POST -> {
                Post post = postRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
                post.setModerationStatus(status);
                post.setUpdatedAt(Instant.now());
                postRepository.save(post);
            }
            case THREAD -> {
                Thread thread = threadRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));
                thread.setModerationStatus(status);
                thread.setUpdatedAt(Instant.now());
                threadRepository.save(thread);
            }
            case ANSWER -> {
                Answer answer = answerRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Answer not found"));
                answer.setModerationStatus(status);
                answer.setUpdatedAt(Instant.now());
                answerRepository.save(answer);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid target type");
        }

        UUID actorId = SecurityUtils.getActorId();
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        ModerationLog log = new ModerationLog();
        log.setId(UUID.randomUUID());
        log.setTargetType(type);
        log.setTargetId(id);
        log.setAction(action);
        log.setActorId(actorId);
        log.setReason(reason);
        log.setCreatedAt(Instant.now());
        logRepository.save(log);
    }
}
