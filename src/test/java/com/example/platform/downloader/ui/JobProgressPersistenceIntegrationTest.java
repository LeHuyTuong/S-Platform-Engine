package com.example.platform.downloader.ui;

import com.example.platform.bootstrap.PlatformApplication;
import com.example.platform.downloader.application.DownloadArtifactService;
import com.example.platform.downloader.domain.entity.Job;
import com.example.platform.downloader.domain.enums.JobState;
import com.example.platform.downloader.domain.enums.Platform;
import com.example.platform.downloader.infrastructure.JobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PlatformApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "app.worker.enabled=false",
        "spring.task.scheduling.enabled=false"
})
class JobProgressPersistenceIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private DownloadArtifactService downloadArtifactService;

    @Test
    void persistRuntimeProgressStoresProgressForFreshDbReads() {
        Job job = new Job("https://www.youtube.com/watch?v=abc123");
        job.setPlatform(Platform.YOUTUBE);
        job.setState(JobState.RUNNING);
        job.setStatus(Job.JobStatus.RUNNING);
        job = jobRepository.save(job);

        job.setProgressPercent(42.5);
        job.setDownloadSpeed("1.2MiB/s");
        job.setEta("00:15");
        job.setVideoTitle("Progress Title");
        job.setPlaylistTitle("Playlist Name");
        job.setCurrentItem(2);
        job.setTotalItems(8);
        downloadArtifactService.persistRuntimeProgress(job);

        Job reloaded = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(reloaded.getProgressPercent()).isEqualTo(42.5);
        assertThat(reloaded.getDownloadSpeed()).isEqualTo("1.2MiB/s");
        assertThat(reloaded.getEta()).isEqualTo("00:15");
        assertThat(reloaded.getVideoTitle()).isEqualTo("Progress Title");
        assertThat(reloaded.getPlaylistTitle()).isEqualTo("Playlist Name");
        assertThat(reloaded.getCurrentItem()).isEqualTo(2);
        assertThat(reloaded.getTotalItems()).isEqualTo(8);
    }
}
