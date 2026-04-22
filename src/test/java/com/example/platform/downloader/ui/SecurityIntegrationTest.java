package com.example.platform.downloader.ui;

import com.example.platform.bootstrap.PlatformApplication;
import com.example.platform.downloader.domain.entity.Job;
import com.example.platform.downloader.domain.enums.JobState;
import com.example.platform.downloader.domain.enums.Platform;
import com.example.platform.downloader.domain.enums.SourceType;
import com.example.platform.downloader.infrastructure.JobRepository;
import com.example.platform.modules.user.domain.User;
import com.example.platform.modules.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PlatformApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.worker.enabled=false",
        "app.worker.redis-enabled=false",
        "app.ui.admin-url=http://localhost:5173/admin",
        "app.ui.downloader-url=http://localhost:5173/app/downloader"
})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    private Job job;

    @BeforeEach
    void setUp() {
        User user = userRepository.findByEmail("user@test.com").orElseThrow();
        Job candidate = new Job("https://www.youtube.com/watch?v=abc123");
        candidate.setUser(user);
        candidate.setPlatform(Platform.YOUTUBE);
        candidate.setSourceType(SourceType.DIRECT_URL);
        candidate.setState(JobState.QUEUED);
        candidate.setStatus(Job.JobStatus.PENDING);
        candidate.setDownloadType("VIDEO");
        candidate.setQuality("best");
        candidate.setFormat("mp4");
        candidate.setVideoTitle("Sample");
        job = jobRepository.save(candidate);
    }

    @Test
    void anonymousCannotReadJobStatus() throws Exception {
        mockMvc.perform(get("/downloader/api/status/" + job.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void ownerCanReadJobStatusWithoutSensitiveFields() throws Exception {
        String body = mockMvc.perform(get("/downloader/api/status/" + job.getId()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains(job.getId());
        assertThat(body).doesNotContain("passwordHash");
        assertThat(body).doesNotContain("telegramChatId");
        assertThat(body).doesNotContain("\"user\"");
    }

    @Test
    void formLoginRedirectsAdminToAdminUi() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "admin@test.com")
                        .param("password", "admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("http://localhost:5173/admin"));
    }

    @Test
    void formLoginRedirectsPublisherToDownloaderUi() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "pub@test.com")
                        .param("password", "pub"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("http://localhost:5173/app/downloader"));
    }
}
