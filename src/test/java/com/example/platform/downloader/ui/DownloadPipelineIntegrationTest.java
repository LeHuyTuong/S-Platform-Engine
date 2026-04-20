package com.example.platform.downloader.ui;

import com.example.platform.bootstrap.PlatformApplication;
import com.example.platform.downloader.application.DownloadArtifactService;
import com.example.platform.downloader.application.DownloaderService;
import com.example.platform.downloader.application.OutboxDispatcher;
import com.example.platform.downloader.application.TelegramNotificationService;
import com.example.platform.downloader.domain.Job;
import com.example.platform.downloader.domain.JobState;
import com.example.platform.downloader.domain.OutboxStatus;
import com.example.platform.downloader.domain.SourceRequest;
import com.example.platform.downloader.domain.SourceRequestState;
import com.example.platform.downloader.infrastructure.JobRepository;
import com.example.platform.downloader.infrastructure.OutboxEventRepository;
import com.example.platform.downloader.infrastructure.SourceRequestRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PlatformApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "app.worker.enabled=true",
        "app.worker.redis-enabled=false",
        "spring.task.scheduling.enabled=false"
})
class DownloadPipelineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private SourceRequestRepository sourceRequestRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxDispatcher outboxDispatcher;

    @MockBean
    private DownloaderService downloaderService;

    @MockBean
    private DownloadArtifactService downloadArtifactService;

    @MockBean
    private TelegramNotificationService telegramNotificationService;

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void sourceRequestDispatchesThroughOutboxAndCompletesJob() throws Exception {
        doNothing().when(downloaderService).executeDownload(any(Job.class), any());
        when(downloadArtifactService.listJobFiles(anyString())).thenReturn(List.of());

        String responseBody = mockMvc.perform(post("/downloader/api/source-requests")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceUrl": "https://www.youtube.com/watch?v=abc123",
                                  "platform": "YOUTUBE",
                                  "sourceType": "DIRECT_URL",
                                  "downloadType": "VIDEO",
                                  "quality": "best",
                                  "format": "mp4"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(responseBody);
        String sourceRequestId = json.path("data").path("id").asText();

        SourceRequest sourceRequest = sourceRequestRepository.findById(sourceRequestId).orElseThrow();
        assertThat(sourceRequest.getState()).isEqualTo(SourceRequestState.RESOLVED);

        List<Job> jobs = jobRepository.findBySourceRequestIdOrderByCreatedAtAsc(sourceRequestId);
        assertThat(jobs).hasSize(1);
        Job queuedJob = jobs.get(0);
        assertThat(queuedJob.getState()).isIn(JobState.QUEUED, JobState.RUNNING, JobState.COMPLETED);

        Job completedJob = drainOutboxUntilTerminal(queuedJob.getId());
        assertThat(completedJob.getState()).isEqualTo(JobState.COMPLETED);
        assertThat(completedJob.getStatus()).isEqualTo(Job.JobStatus.COMPLETED);
        assertThat(outboxEventRepository.findAll())
                .extracting(event -> event.getStatus())
                .containsOnly(OutboxStatus.PROCESSED);

        verify(downloaderService).executeDownload(any(Job.class), any());
        verify(telegramNotificationService).notifyJobCompleted(any(Job.class), any(), any());
    }

    private Job drainOutboxUntilTerminal(String jobId) throws InterruptedException {
        Job job = jobRepository.findById(jobId).orElseThrow();
        for (int i = 0; i < 10; i++) {
            outboxDispatcher.dispatchPending();
            job = jobRepository.findById(jobId).orElseThrow();
            boolean noPendingEvents = outboxEventRepository.findAll().stream()
                    .allMatch(event -> event.getStatus() == OutboxStatus.PROCESSED);
            if (job.getState() == JobState.COMPLETED && noPendingEvents) {
                return job;
            }
            Thread.sleep(50L);
        }
        return job;
    }
}
