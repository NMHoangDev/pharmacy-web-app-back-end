package com.backend.content.service;

import com.backend.content.api.dto.AnswerCreateRequest;
import com.backend.content.api.dto.AnswerItem;
import com.backend.content.api.dto.AnswerUpdateRequest;
import com.backend.content.api.dto.UserSummaryDto;
import com.backend.content.model.Answer;
import com.backend.content.model.ContentUser;
import com.backend.content.model.ModerationStatus;
import com.backend.content.model.TargetType;
import com.backend.content.model.Thread;
import com.backend.content.model.Vote;
import com.backend.content.repo.AnswerRepository;
import com.backend.content.repo.ThreadRepository;
import com.backend.content.repo.VoteRepository;
import com.backend.content.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final ThreadRepository threadRepository;
    private final VoteRepository voteRepository;
    private final ThreadService threadService;
    private final ContentUserService userService;
    private final ObjectMapper objectMapper;

    public AnswerService(AnswerRepository answerRepository,
            ThreadRepository threadRepository,
            VoteRepository voteRepository,
            ThreadService threadService,
            ContentUserService userService,
            ObjectMapper objectMapper) {
        this.answerRepository = answerRepository;
        this.threadRepository = threadRepository;
        this.voteRepository = voteRepository;
        this.threadService = threadService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    public AnswerItem create(UUID threadId, AnswerCreateRequest req) {
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));
        if (thread.getThreadStatus().name().equals("CLOSED")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Thread is closed");
        }

        UUID actorId = SecurityUtils.getActorId();
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        String displayName = SecurityUtils.getDisplayName();
        String role = SecurityUtils.getPrimaryRole();
        ContentUser user = userService.ensureUser(actorId, displayName, role);

        Answer answer = new Answer();
        answer.setId(UUID.randomUUID());
        answer.setThreadId(threadId);
        answer.setAuthorId(actorId);
        answer.setContent(req.content());
        answer.setReferencesJson(toJson(req));
        answer.setModerationStatus(ModerationStatus.PUBLISHED);
        answer.setPinned(false);
        answer.setBestAnswer(false);
        answer.setCreatedAt(Instant.now());
        answer.setUpdatedAt(Instant.now());
        answerRepository.save(answer);

        boolean isPharmacist = SecurityUtils.getRoles().contains("PHARMACIST");
        threadService.touchAfterAnswer(thread, isPharmacist);

        UserSummaryDto authorDto = userService.toSummary(user, false);
        return new AnswerItem(answer.getId(), answer.getContent(), authorDto, answer.isPinned(),
                answer.isBestAnswer(), answer.getCreatedAt());
    }

    public AnswerItem update(UUID answerId, AnswerUpdateRequest req, boolean isModerator) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Answer not found"));
        UUID actorId = SecurityUtils.getActorId();
        if (!isModerator) {
            if (actorId == null || !actorId.equals(answer.getAuthorId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
            }
        }
        answer.setContent(req.content());
        answer.setReferencesJson(toJson(req));
        answer.setUpdatedAt(Instant.now());
        answerRepository.save(answer);

        ContentUser author = userService.findByIds(List.of(answer.getAuthorId())).get(answer.getAuthorId());
        return new AnswerItem(answer.getId(), answer.getContent(), userService.toSummary(author, false),
                answer.isPinned(), answer.isBestAnswer(), answer.getCreatedAt());
    }

    public AnswerItem pin(UUID answerId, boolean isModerator) {
        if (!isModerator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Answer not found"));
        answer.setPinned(true);
        answer.setUpdatedAt(Instant.now());
        answerRepository.save(answer);
        ContentUser author = userService.findByIds(List.of(answer.getAuthorId())).get(answer.getAuthorId());
        return new AnswerItem(answer.getId(), answer.getContent(), userService.toSummary(author, false),
                answer.isPinned(), answer.isBestAnswer(), answer.getCreatedAt());
    }

    public AnswerItem best(UUID answerId, boolean allow) {
        if (!allow) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Answer not found"));
        List<Answer> all = answerRepository.findByThreadId(answer.getThreadId());
        for (Answer a : all) {
            boolean shouldBeBest = a.getId().equals(answerId);
            if (a.isBestAnswer() != shouldBeBest) {
                a.setBestAnswer(shouldBeBest);
                a.setUpdatedAt(Instant.now());
            }
        }
        answer.setBestAnswer(true);
        answer.setUpdatedAt(Instant.now());
        answerRepository.saveAll(all);
        ContentUser author = userService.findByIds(List.of(answer.getAuthorId())).get(answer.getAuthorId());
        return new AnswerItem(answer.getId(), answer.getContent(), userService.toSummary(author, false),
                answer.isPinned(), answer.isBestAnswer(), answer.getCreatedAt());
    }

    public void vote(UUID answerId, int value) {
        if (value != 1 && value != -1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vote value invalid");
        }
        UUID actorId = SecurityUtils.getActorId();
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        answerRepository.findById(answerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Answer not found"));

        Vote vote = voteRepository.findByTargetTypeAndTargetIdAndUserId(TargetType.ANSWER, answerId, actorId)
                .orElseGet(Vote::new);
        if (vote.getId() == null) {
            vote.setId(UUID.randomUUID());
            vote.setTargetType(TargetType.ANSWER);
            vote.setTargetId(answerId);
            vote.setUserId(actorId);
            vote.setCreatedAt(Instant.now());
        }
        vote.setValue(value);
        voteRepository.save(vote);
    }

    public UUID getThreadId(UUID answerId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Answer not found"));
        return answer.getThreadId();
    }

    private String toJson(AnswerCreateRequest req) {
        if (req == null || req.references() == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(req.references());
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "References invalid");
        }
    }

    private String toJson(AnswerUpdateRequest req) {
        if (req == null || req.references() == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(req.references());
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "References invalid");
        }
    }
}
