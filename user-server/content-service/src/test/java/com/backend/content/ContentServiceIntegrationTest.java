package com.backend.content;

import com.backend.content.api.dto.AnswerCreateRequest;
import com.backend.content.api.dto.QuestionCreateRequest;
import com.backend.content.api.dto.QuestionContext;
import com.backend.content.api.dto.VoteRequest;
import com.backend.content.model.Answer;
import com.backend.content.model.ContentUser;
import com.backend.content.model.ModerationStatus;
import com.backend.content.model.Post;
import com.backend.content.model.Thread;
import com.backend.content.model.ThreadStatus;
import com.backend.content.repo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class ContentServiceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("content_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void mysqlProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> 1);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ContentUserRepository userRepository;

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Test
    void listPostsReturnsPagination() throws Exception {
        ContentUser user = seedUser("Author", "ADMIN");
        for (int i = 1; i <= 12; i++) {
            Post post = new Post();
            post.setId(UUID.randomUUID());
            post.setSlug("post-" + i);
            post.setTitle("Post " + i);
            post.setExcerpt("Excerpt " + i);
            post.setAuthorId(user.getId());
            post.setModerationStatus(ModerationStatus.PUBLISHED);
            post.setPublishedAt(Instant.now());
            post.setCreatedAt(Instant.now());
            post.setUpdatedAt(Instant.now());
            postRepository.save(post);
        }

        mockMvc.perform(get("/api/content/posts?page=1&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(10))
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.pageSize").value(10))
                .andExpect(jsonPath("$.pagination.total").value(12));
    }

    @Test
    void listPublicPostsIgnoresInvalidBearerToken() throws Exception {
        ContentUser user = seedUser("Author", "ADMIN");
        Post post = new Post();
        post.setId(UUID.randomUUID());
        post.setSlug("public-post");
        post.setTitle("Public Post");
        post.setExcerpt("Excerpt");
        post.setAuthorId(user.getId());
        post.setModerationStatus(ModerationStatus.PUBLISHED);
        post.setPublishedAt(Instant.now());
        post.setCreatedAt(Instant.now());
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);

        mockMvc.perform(get("/api/content/public/posts?page=1&pageSize=10&sortBy=publishedAt&sortDir=desc")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[*].slug").value(org.hamcrest.Matchers.hasItem("public-post")));
    }

    @Test
    void createPostRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/content/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPostWithInvalidBearerTokenStillFails() throws Exception {
        mockMvc.perform(post("/api/content/posts")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createQuestionAndAnswerUpdatesCounters() throws Exception {
        UUID userId = UUID.randomUUID();
        String createBody = objectMapper.writeValueAsString(new QuestionCreateRequest(
                "Hỏi đáp",
                "Nội dung câu hỏi",
                false,
                List.of(),
                new QuestionContext(25, "FEMALE", false, false, null, List.of(), List.of(), "NORMAL")));

        String response = mockMvc.perform(post("/api/content/questions")
                .with(jwt().jwt(jwt -> jwt.subject(userId.toString())
                        .claim("name", "User A")
                        .claim("realm_access", java.util.Map.of("roles", List.of("USER")))))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID threadId = UUID.fromString(objectMapper.readTree(response).get("id").asText());
        Thread thread = threadRepository.findById(threadId).orElseThrow();
        assertThat(thread.getAnswerCount()).isZero();

        UUID pharmacistId = UUID.randomUUID();
        String answerBody = objectMapper.writeValueAsString(new AnswerCreateRequest("Trả lời", List.of()));

        mockMvc.perform(post("/api/content/questions/" + thread.getId() + "/answers")
                .with(jwt().jwt(jwt -> jwt.subject(pharmacistId.toString())
                        .claim("name", "Pharmacist")
                        .claim("realm_access", java.util.Map.of("roles", List.of("PHARMACIST")))))
                .contentType(MediaType.APPLICATION_JSON)
                .content(answerBody))
                .andExpect(status().isOk());

        Thread updated = threadRepository.findById(thread.getId()).orElseThrow();
        assertThat(updated.getAnswerCount()).isEqualTo(1);
        assertThat(updated.isHasPharmacistAnswer()).isTrue();
    }

    @Test
    void voteUsesUniqueConstraint() throws Exception {
        ContentUser user = seedUser("Voter", "USER");
        Thread thread = seedThread(user.getId());
        Answer answer = seedAnswer(thread.getId(), user.getId());

        VoteRequest up = new VoteRequest(1);
        VoteRequest down = new VoteRequest(-1);

        mockMvc.perform(post("/api/content/answers/" + answer.getId() + "/vote")
                .with(jwt().jwt(jwt -> jwt.subject(user.getId().toString())
                        .claim("realm_access", java.util.Map.of("roles", List.of("USER")))))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(up)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/content/answers/" + answer.getId() + "/vote")
                .with(jwt().jwt(jwt -> jwt.subject(user.getId().toString())
                        .claim("realm_access", java.util.Map.of("roles", List.of("USER")))))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(down)))
                .andExpect(status().isOk());

        assertThat(voteRepository.findAll()).hasSize(1);
        assertThat(voteRepository.findAll().get(0).getValue()).isEqualTo(-1);
    }

    private ContentUser seedUser(String name, String role) {
        ContentUser user = new ContentUser();
        user.setId(UUID.randomUUID());
        user.setDisplayName(name);
        user.setRole(role);
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }

    private Thread seedThread(UUID userId) {
        Thread thread = new Thread();
        thread.setId(UUID.randomUUID());
        thread.setSlug("thread-" + UUID.randomUUID());
        thread.setTitle("Thread");
        thread.setContent("Content");
        thread.setAskerId(userId);
        thread.setAnonymous(false);
        thread.setModerationStatus(ModerationStatus.PUBLISHED);
        thread.setThreadStatus(ThreadStatus.OPEN);
        thread.setCreatedAt(Instant.now());
        thread.setUpdatedAt(Instant.now());
        thread.setLastActivityAt(Instant.now());
        thread.setAnswerCount(0);
        thread.setHasPharmacistAnswer(false);
        return threadRepository.save(thread);
    }

    private Answer seedAnswer(UUID threadId, UUID authorId) {
        Answer answer = new Answer();
        answer.setId(UUID.randomUUID());
        answer.setThreadId(threadId);
        answer.setAuthorId(authorId);
        answer.setContent("Answer");
        answer.setModerationStatus(ModerationStatus.PUBLISHED);
        answer.setPinned(false);
        answer.setBestAnswer(false);
        answer.setCreatedAt(Instant.now());
        answer.setUpdatedAt(Instant.now());
        return answerRepository.save(answer);
    }
}
