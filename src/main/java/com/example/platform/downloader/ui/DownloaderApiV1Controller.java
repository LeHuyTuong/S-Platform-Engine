package com.example.platform.downloader.ui;

import com.example.platform.downloader.application.DownloadAccessPolicyService;
import com.example.platform.downloader.application.DownloadArtifactService;
import com.example.platform.downloader.application.DownloaderDtoMapper;
import com.example.platform.downloader.application.DownloaderService;
import com.example.platform.downloader.application.SourceRequestService;
import com.example.platform.downloader.application.UserConnectionSettingsService;
import com.example.platform.downloader.application.job.JobManager;
import com.example.platform.downloader.domain.RuntimeSettings;
import com.example.platform.downloader.domain.entity.Job;
import com.example.platform.downloader.domain.entity.SourceRequest;
import com.example.platform.downloader.domain.enums.JobState;
import com.example.platform.downloader.domain.enums.Platform;
import com.example.platform.downloader.infrastructure.JobRepository;
import com.example.platform.downloader.infrastructure.SourceRequestRepository;
import com.example.platform.downloader.ui.dto.JobFileResponse;
import com.example.platform.downloader.ui.dto.JobStatusResponse;
import com.example.platform.downloader.ui.dto.RuntimeSettingsPayload;
import com.example.platform.downloader.ui.dto.RuntimeSettingsStatusResponse;
import com.example.platform.downloader.ui.dto.SourceRequestResponse;
import com.example.platform.downloader.ui.dto.SubmitSourceRequest;
import com.example.platform.kernel.exception.BusinessException;
import com.example.platform.kernel.exception.ResourceNotFoundException;
import com.example.platform.kernel.ui.RestResponse;
import com.example.platform.modules.user.domain.User;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class DownloaderApiV1Controller {

    private final DownloaderService downloaderService;
    private final DownloadArtifactService downloadArtifactService;
    private final DownloadAccessPolicyService accessPolicyService;
    private final JobManager jobManager;
    private final JobRepository jobRepository;
    private final SourceRequestRepository sourceRequestRepository;
    private final SourceRequestService sourceRequestService;
    private final DownloaderDtoMapper dtoMapper;
    private final UserConnectionSettingsService userConnectionSettingsService;

    public DownloaderApiV1Controller(DownloaderService downloaderService,
                                     DownloadArtifactService downloadArtifactService,
                                     DownloadAccessPolicyService accessPolicyService,
                                     JobManager jobManager,
                                     JobRepository jobRepository,
                                     SourceRequestRepository sourceRequestRepository,
                                     SourceRequestService sourceRequestService,
                                     DownloaderDtoMapper dtoMapper,
                                     UserConnectionSettingsService userConnectionSettingsService) {
        this.downloaderService = downloaderService;
        this.downloadArtifactService = downloadArtifactService;
        this.accessPolicyService = accessPolicyService;
        this.jobManager = jobManager;
        this.jobRepository = jobRepository;
        this.sourceRequestRepository = sourceRequestRepository;
        this.sourceRequestService = sourceRequestService;
        this.dtoMapper = dtoMapper;
        this.userConnectionSettingsService = userConnectionSettingsService;
    }

    @PostMapping("/source-requests")
    public RestResponse<SourceRequestResponse> submitSourceRequest(@RequestBody SubmitSourceRequest request,
                                                                   Principal principal) {
        User user = accessPolicyService.currentUser(principal);
        SourceRequestService.SubmissionResult result;
        try {
            synchronized (jobManager.getUserLock(user.getId().toString())) {
                accessPolicyService.enforceSubmissionPolicy(user, request);
                result = sourceRequestService.submit(request, user);
            }
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        }

        List<JobStatusResponse> jobs = new ArrayList<>();
        if (result.primaryJob() != null) {
            jobs.add(toJobResponse(result.primaryJob()));
        }
        return RestResponse.ok(dtoMapper.toSourceRequest(result.sourceRequest(), jobs), "Source request accepted");
    }

    @GetMapping("/source-requests")
    public RestResponse<List<SourceRequestResponse>> listSourceRequests(Principal principal,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size) {
        User user = accessPolicyService.currentUser(principal);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SourceRequest> results = sourceRequestRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        Map<String, List<JobStatusResponse>> jobsBySourceRequestId = listJobSummariesBySourceRequest(results.getContent());

        List<SourceRequestResponse> data = results.getContent().stream()
                .map(sourceRequest -> dtoMapper.toSourceRequest(
                        sourceRequest,
                        jobsBySourceRequestId.getOrDefault(sourceRequest.getId(), List.of())
                ))
                .toList();

        RestResponse<List<SourceRequestResponse>> response = RestResponse.ok(data);
        response.setMeta(pageMeta(results));
        return response;
    }

    @GetMapping("/source-requests/{id}")
    public RestResponse<SourceRequestResponse> getSourceRequest(@PathVariable String id, Principal principal) {
        User user = accessPolicyService.currentUser(principal);
        SourceRequest sourceRequest = sourceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Source request not found"));
        accessPolicyService.ensureOwnerOrAdmin(sourceRequest.getUser(), user);

        List<JobStatusResponse> jobs = jobRepository.findBySourceRequestIdOrderByCreatedAtAsc(id).stream()
                .map(this::toJobResponse)
                .toList();
        return RestResponse.ok(dtoMapper.toSourceRequest(sourceRequest, jobs));
    }

    @GetMapping("/jobs")
    public RestResponse<List<JobStatusResponse>> listJobs(Principal principal,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          @RequestParam(required = false) JobState state,
                                                          @RequestParam(required = false) Job.JobStatus status,
                                                          @RequestParam(required = false) Platform platform) {
        User user = accessPolicyService.currentUser(principal);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Job> results = jobRepository.searchJobs(user.getId(), state, status, platform, pageable);
        List<JobStatusResponse> data = results.getContent().stream()
                .map(this::toJobSummaryResponse)
                .toList();

        RestResponse<List<JobStatusResponse>> response = RestResponse.ok(data);
        response.setMeta(pageMeta(results));
        return response;
    }

    @GetMapping("/jobs/{id}")
    public RestResponse<JobStatusResponse> getJob(@PathVariable String id, Principal principal) {
        Job job = jobManager.getJob(id);
        if (job == null) {
            throw new ResourceNotFoundException("Job not found: " + id);
        }
        User user = accessPolicyService.currentUser(principal);
        accessPolicyService.ensureOwnerOrAdmin(job.getUser(), user);
        return RestResponse.ok(toJobResponse(job));
    }

    @GetMapping("/jobs/{id}/logs")
    public RestResponse<List<String>> getJobLogs(@PathVariable String id, Principal principal) {
        Job job = jobManager.getJob(id);
        if (job == null) {
            throw new ResourceNotFoundException("Job not found: " + id);
        }
        User user = accessPolicyService.currentUser(principal);
        accessPolicyService.ensureOwnerOrAdmin(job.getUser(), user);
        return RestResponse.ok(job.getLogs());
    }

    @GetMapping("/jobs/{jobId}/files")
    public RestResponse<List<JobFileResponse>> listFilesByJob(@PathVariable String jobId, Principal principal) {
        Job job = jobManager.getJob(jobId);
        if (job == null) {
            throw new ResourceNotFoundException("Job not found");
        }
        User user = accessPolicyService.currentUser(principal);
        accessPolicyService.ensureOwnerOrAdmin(job.getUser(), user);

        List<JobFileResponse> files = downloadArtifactService.listJobFiles(jobId).stream()
                .map(file -> toJobFileResponse(jobId, file))
                .toList();
        return RestResponse.ok(files);
    }

    @GetMapping("/jobs/{jobId}/files/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String jobId,
                                                 @PathVariable String filename,
                                                 Principal principal) throws IOException {
        Job job = jobManager.getJob(jobId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        User user = accessPolicyService.currentUser(principal);
        accessPolicyService.ensureOwnerOrAdmin(job.getUser(), user);

        ResponseEntity<Resource> response = downloadArtifactService.serveFile(jobId, filename);
        return response != null ? response : ResponseEntity.notFound().build();
    }

    @GetMapping("/provider-credentials/status")
    public RestResponse<Map<String, Boolean>> getProviderCredentialStatus(Principal principal) {
        User user = accessPolicyService.currentUser(principal);
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (Platform platform : Platform.values()) {
            result.put(platform.name(), downloaderService.hasProviderCookie(user, platform));
        }
        return RestResponse.ok(result);
    }

    @PostMapping("/provider-credentials/{platform}/cookies")
    public RestResponse<Void> uploadProviderCookie(@PathVariable String platform,
                                                   @RequestParam("file") MultipartFile file,
                                                   Principal principal) {
        User user = accessPolicyService.currentUser(principal);
        try {
            downloaderService.saveProviderCookie(file, user, parsePlatform(platform));
            return RestResponse.ok(null, "Cookie uploaded successfully");
        } catch (Exception e) {
            throw new BusinessException("Upload failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/provider-credentials/{platform}/cookies")
    public RestResponse<Void> deleteProviderCookie(@PathVariable String platform, Principal principal) {
        User user = accessPolicyService.currentUser(principal);
        downloaderService.deleteProviderCookie(user, parsePlatform(platform));
        return RestResponse.ok(null, "Cookie deleted");
    }

    @GetMapping("/runtime-settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLISHER')")
    public RestResponse<RuntimeSettingsStatusResponse> getSettingsStatus(Principal principal) {
        User user = accessPolicyService.currentUser(principal);
        RuntimeSettings masked = userConnectionSettingsService.getMasked(user);
        RuntimeSettingsStatusResponse response = new RuntimeSettingsStatusResponse();
        response.setHasSettings(masked.isValid());
        response.setHasTelegramToken(masked.hasTelegramBotToken());
        response.setHasTelegramChatId(masked.hasTelegramChatId());
        response.setHasGoogleDriveServiceAccount(masked.hasGoogleDriveServiceAccountJson());
        response.setHasGoogleDriveFolderId(masked.hasGoogleDriveFolderId());
        response.setHasBaseUrl(masked.hasBaseUrl());
        return RestResponse.ok(response);
    }

    @PutMapping("/runtime-settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLISHER')")
    public RestResponse<Void> saveSettings(@RequestBody RuntimeSettingsPayload payload, Principal principal) {
        User user = accessPolicyService.currentUser(principal);
        try {
            userConnectionSettingsService.save(payload.toRuntimeSettings(), user);
            return RestResponse.ok(null, "Settings saved successfully");
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    @DeleteMapping("/runtime-settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLISHER')")
    public RestResponse<Void> clearSettings(Principal principal) {
        userConnectionSettingsService.clear(accessPolicyService.currentUser(principal));
        return RestResponse.ok(null, "Settings cleared");
    }

    private JobStatusResponse toJobResponse(Job job) {
        Job hydrated = jobManager.getJob(job.getId());
        if (hydrated != null) {
            job.setLogs(hydrated.getLogs());
        } else {
            job.setLogs(List.of());
        }
        return dtoMapper.toJobStatus(job, job.getLogs());
    }

    private JobStatusResponse toJobSummaryResponse(Job job) {
        return dtoMapper.toJobStatus(job, List.of());
    }

    private JobFileResponse toJobFileResponse(String jobId, Map<String, String> file) {
        String filename = file.get("name");
        String encodedFilename = UriUtils.encodePathSegment(filename, StandardCharsets.UTF_8);
        return dtoMapper.toJobFile(
                filename,
                null,
                "/api/v1/jobs/" + jobId + "/files/" + encodedFilename,
                file.get("contentType"),
                file.get("type"),
                Long.parseLong(file.getOrDefault("size", "0"))
        );
    }

    private Map<String, List<JobStatusResponse>> listJobSummariesBySourceRequest(List<SourceRequest> sourceRequests) {
        if (sourceRequests.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> sourceRequestIds = sourceRequests.stream()
                .map(SourceRequest::getId)
                .toList();

        return jobRepository.findBySourceRequestIdInOrderBySourceRequestIdAscCreatedAtAsc(sourceRequestIds).stream()
                .collect(Collectors.groupingBy(
                        job -> job.getSourceRequest().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(Function.identity(), Collectors.toList())
                ))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(this::toJobSummaryResponse)
                                .toList(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Platform parsePlatform(String value) {
        return Platform.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private Map<String, Object> pageMeta(Page<?> page) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("page", page.getNumber());
        meta.put("size", page.getSize());
        meta.put("totalItems", page.getTotalElements());
        meta.put("totalPages", page.getTotalPages());
        meta.put("hasNext", page.hasNext());
        meta.put("hasPrevious", page.hasPrevious());
        return meta;
    }
}
