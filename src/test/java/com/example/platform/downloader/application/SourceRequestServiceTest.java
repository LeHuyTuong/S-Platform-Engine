package com.example.platform.downloader.application;

import com.example.platform.downloader.application.provider.ContentProvider;
import com.example.platform.downloader.application.provider.ProviderRegistry;
import com.example.platform.downloader.domain.Job;
import com.example.platform.downloader.domain.JobState;
import com.example.platform.downloader.domain.Platform;
import com.example.platform.downloader.domain.SourceRequest;
import com.example.platform.downloader.domain.SourceRequestState;
import com.example.platform.downloader.domain.SourceType;
import com.example.platform.downloader.infrastructure.JobRepository;
import com.example.platform.downloader.infrastructure.SourceRequestRepository;
import com.example.platform.downloader.ui.dto.SubmitSourceRequest;
import com.example.platform.modules.user.domain.Role;
import com.example.platform.modules.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SourceRequestServiceTest {

    @Mock
    private SourceRequestRepository sourceRequestRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ProviderRegistry providerRegistry;

    @Mock
    private OutboxService outboxService;

    @Mock
    private ContentProvider contentProvider;

    @InjectMocks
    private SourceRequestService sourceRequestService;

    @Test
    void submitDirectUrlCreatesQueuedJobAndResolvedSourceRequest() {
        User user = new User("user@test.com", "hash", Role.USER, true);
        user.setId(42L);

        SubmitSourceRequest request = new SubmitSourceRequest();
        request.setSourceUrl("https://www.youtube.com/watch?v=abc123");
        request.setDownloadType("VIDEO");
        request.setQuality("best");
        request.setFormat("mp4");

        when(providerRegistry.detect(request.getSourceUrl())).thenReturn(contentProvider);
        when(contentProvider.platform()).thenReturn(Platform.YOUTUBE);
        when(sourceRequestRepository.save(any(SourceRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SourceRequestService.SubmissionResult result = sourceRequestService.submit(request, user);

        assertThat(result.sourceRequest().getState()).isEqualTo(SourceRequestState.RESOLVED);
        assertThat(result.sourceRequest().getSourceType()).isEqualTo(SourceType.DIRECT_URL);
        assertThat(result.primaryJob()).isNotNull();
        assertThat(result.primaryJob().getPlatform()).isEqualTo(Platform.YOUTUBE);
        assertThat(result.primaryJob().getState()).isEqualTo(JobState.QUEUED);

        verify(outboxService).create(eq("job"), eq(result.primaryJob().getId()), eq("DOWNLOAD_JOB_QUEUED"), any(Map.class));
        verify(outboxService, never()).create(eq("source_request"), eq(result.sourceRequest().getId()), eq("SOURCE_REQUEST_ACCEPTED"), any(Map.class));
    }
}
