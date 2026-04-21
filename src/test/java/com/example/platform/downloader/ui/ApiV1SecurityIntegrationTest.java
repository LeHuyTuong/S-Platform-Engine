package com.example.platform.downloader.ui;

import com.example.platform.bootstrap.PlatformApplication;
import com.example.platform.downloader.application.DownloadArtifactService;
import com.example.platform.downloader.domain.entity.Job;
import com.example.platform.downloader.domain.enums.JobState;
import com.example.platform.downloader.domain.enums.Platform;
import com.example.platform.downloader.domain.enums.SourceType;
import com.example.platform.downloader.infrastructure.JobRepository;
import com.example.platform.modules.user.domain.User;
import com.example.platform.modules.user.infrastructure.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PlatformApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.worker.enabled=false",
        "app.worker.redis-enabled=false"
})
class ApiV1SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private DownloadArtifactService downloadArtifactService;

    private Job job;

    @BeforeEach
    void setUp() {
        User user = userRepository.findByEmail("user@test.com").orElseThrow();
        Job candidate = new Job("https://www.youtube.com/watch?v=api-v1-test");
        candidate.setUser(user);
        candidate.setPlatform(Platform.YOUTUBE);
        candidate.setSourceType(SourceType.DIRECT_URL);
        candidate.setState(JobState.QUEUED);
        candidate.setStatus(Job.JobStatus.PENDING);
        candidate.setDownloadType("VIDEO");
        candidate.setQuality("best");
        candidate.setFormat("mp4");
        candidate.setVideoTitle("API V1 Sample");
        job = jobRepository.save(candidate);
    }

    @AfterEach
    void tearDown() {
        reset(downloadArtifactService);
    }

    @Test
    void anonymousApiV1RequestReturnsJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void anonymousMeEndpointReturnsUnauthenticatedPayload() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.authenticated").value(false));
    }

    @Test
    void loginCreatesSessionAndAllowsFetchingOwnJobs() throws Exception {
        MockHttpSession session = loginAs("user@test.com", "user");

        String body = mockMvc.perform(get("/api/v1/jobs").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.meta.totalItems").isNumber())
                .andExpect(jsonPath("$.data[0].logs", hasSize(0)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains(job.getId());
        assertThat(body).contains("API V1 Sample");
    }

    @Test
    void invalidPaginationReturnsBusinessErrorEnvelope() throws Exception {
        MockHttpSession session = loginAs("user@test.com", "user");

        mockMvc.perform(get("/api/v1/jobs?page=-1&size=20").session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.message").value("page must be greater than or equal to 0"));
    }

    @Test
    void oversizedPaginationReturnsBusinessErrorEnvelope() throws Exception {
        MockHttpSession session = loginAs("user@test.com", "user");

        mockMvc.perform(get("/api/v1/source-requests?page=0&size=101").session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.message").value("size must be between 1 and 100"));
    }

    @Test
    void userCannotReadRuntimeSettingsApi() throws Exception {
        MockHttpSession session = loginAs("user@test.com", "user");

        mockMvc.perform(get("/api/v1/runtime-settings").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void csrfEndpointIssuesTokenForFrontendLoginFlow() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.data.parameterName").isString());
    }

    @Test
    void loginWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@test.com",
                                  "password": "user"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void fileListingEncodesDownloadUrlPathSegments() throws Exception {
        when(downloadArtifactService.listJobFiles(anyString())).thenReturn(List.of(
                Map.of(
                        "name", "video #1 100%.mp4",
                        "contentType", "video/mp4",
                        "type", "video",
                        "size", "128"
                )
        ));
        MockHttpSession session = loginAs("user@test.com", "user");

        mockMvc.perform(get("/api/v1/jobs/{jobId}/files", job.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].downloadUrl")
                        .value("/api/v1/jobs/" + job.getId() + "/files/video%20%231%20100%25.mp4"));
    }

    private MockHttpSession loginAs(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.authenticated").value(true))
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
