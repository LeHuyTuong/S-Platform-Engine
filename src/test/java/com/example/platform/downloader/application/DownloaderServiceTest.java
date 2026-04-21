package com.example.platform.downloader.application;

import com.example.platform.downloader.application.job.JobEventService;
import com.example.platform.downloader.application.provider.ProviderRegistry;
import com.example.platform.downloader.domain.entity.Job;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DownloaderServiceTest {

    @Mock
    private ProviderRegistry providerRegistry;

    @Mock
    private DownloadArtifactService downloadArtifactService;

    @Mock
    private JobEventService jobEventService;

    @Mock
    private ProviderCredentialService providerCredentialService;

    @Test
    void persistRuntimeProgressIsThrottledUntilIntervalExpires() {
        TestableDownloaderService service = new TestableDownloaderService(
                providerRegistry,
                downloadArtifactService,
                jobEventService,
                providerCredentialService
        );
        Job job = new Job("https://example.com/video");

        service.now = 1_000L;
        service.persist(job);

        service.now = 1_200L;
        service.persist(job);

        service.now = 1_600L;
        service.persist(job);

        verify(downloadArtifactService, times(2)).persistRuntimeProgress(job);
    }

    @Test
    void clearPersistTimerAllowsImmediatePersistAgain() {
        TestableDownloaderService service = new TestableDownloaderService(
                providerRegistry,
                downloadArtifactService,
                jobEventService,
                providerCredentialService
        );
        Job job = new Job("https://example.com/video");

        service.now = 5_000L;
        service.persist(job);

        service.now = 5_100L;
        service.persist(job);

        service.clear(job.getId());

        service.now = 5_150L;
        service.persist(job);

        verify(downloadArtifactService, times(2)).persistRuntimeProgress(job);
    }

    private static final class TestableDownloaderService extends DownloaderService {

        private long now;

        private TestableDownloaderService(ProviderRegistry providerRegistry,
                                          DownloadArtifactService downloadArtifactService,
                                          JobEventService jobEventService,
                                          ProviderCredentialService providerCredentialService) {
            super(providerRegistry, downloadArtifactService, jobEventService, providerCredentialService, "downloads");
        }

        private void persist(Job job) {
            persistRuntimeProgress(job);
        }

        private void clear(String jobId) {
            clearPersistTimer(jobId);
        }

        @Override
        protected long currentTimeMillis() {
            return now;
        }
    }
}
