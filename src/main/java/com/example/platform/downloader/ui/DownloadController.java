package com.example.platform.downloader.ui;

import com.example.platform.downloader.application.DownloadAccessPolicyService;
import com.example.platform.downloader.application.DownloadArtifactService;
import com.example.platform.downloader.application.DownloaderDtoMapper;
import com.example.platform.downloader.application.DownloaderService;
import com.example.platform.downloader.application.job.JobManager;
import com.example.platform.downloader.application.SourceRequestService;
import com.example.platform.downloader.domain.entity.Job;
import com.example.platform.downloader.domain.enums.Platform;
import com.example.platform.downloader.domain.entity.SourceRequest;
import com.example.platform.downloader.infrastructure.JobRepository;
import com.example.platform.downloader.infrastructure.SourceRequestRepository;
import com.example.platform.downloader.ui.dto.JobFileResponse;
import com.example.platform.downloader.ui.dto.JobStatusResponse;
import com.example.platform.downloader.ui.dto.SourceRequestResponse;
import com.example.platform.downloader.ui.dto.SubmitSourceRequest;
import com.example.platform.kernel.exception.BusinessException;
import com.example.platform.kernel.exception.ResourceNotFoundException;
import com.example.platform.kernel.ui.RestResponse;
import com.example.platform.modules.user.domain.User;
import com.example.platform.modules.user.infrastructure.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/downloader")
public class DownloadController {

    private final DownloaderService downloaderService;
    private final DownloadArtifactService downloadArtifactService;
    private final DownloadAccessPolicyService accessPolicyService;
    private final JobManager jobManager;
    private final JobRepository jobRepository;
    private final SourceRequestRepository sourceRequestRepository;
    private final SourceRequestService sourceRequestService;
    private final DownloaderDtoMapper dtoMapper;
    private final UserRepository userRepository;

    public DownloadController(DownloaderService downloaderService,
                              DownloadArtifactService downloadArtifactService,
                              DownloadAccessPolicyService accessPolicyService,
                              JobManager jobManager,
                              JobRepository jobRepository,
                              SourceRequestRepository sourceRequestRepository,
                              SourceRequestService sourceRequestService,
                              DownloaderDtoMapper dtoMapper,
                              UserRepository userRepository) {
        this.downloaderService = downloaderService;
        this.downloadArtifactService = downloadArtifactService;
        this.accessPolicyService = accessPolicyService;
        this.jobManager = jobManager;
        this.jobRepository = jobRepository;
        this.sourceRequestRepository = sourceRequestRepository;
        this.sourceRequestService = sourceRequestService;
        this.dtoMapper = dtoMapper;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String index(Model model, Principal principal) {
        if (principal != null) {
            userRepository.findByEmail(principal.getName()).ifPresentOrElse(user -> {
                model.addAttribute("jobs", jobRepository.findByUserOrderByCreatedAtDesc(user));
                model.addAttribute("currentUser", user);
                model.addAttribute("hasCookies", downloaderService.hasCookieFile(user));
            }, () -> {
                model.addAttribute("jobs", Collections.emptyList());
                model.addAttribute("hasCookies", false);
            });
        } else {
            model.addAttribute("jobs", Collections.emptyList());
            model.addAttribute("hasCookies", false);
        }
        return "downloader";
    }

    @PostMapping("/api/source-requests")
    @ResponseBody
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

    @PostMapping("/api/submit")
    @ResponseBody
    public RestResponse<JobStatusResponse> submitCompatibility(@RequestBody SubmitSourceRequest request,
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
        JobStatusResponse response;
        if (result.primaryJob() != null) {
            response = toJobResponse(result.primaryJob());
        } else {
            response = new JobStatusResponse();
            response.setId(result.sourceRequest().getId());
            response.setSourceRequestId(result.sourceRequest().getId());
            response.setStatus("PENDING");
            response.setState(result.sourceRequest().getState().name());
            response.setPlatform(result.sourceRequest().getPlatform().name());
            response.setSourceType(result.sourceRequest().getSourceType().name());
            response.setUrl(result.sourceRequest().getSourceUrl());
        }
        return RestResponse.ok(response, "Job submitted successfully");
    }

    @GetMapping("/api/source-requests/{id}")
    @ResponseBody
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

    @GetMapping("/api/jobs/{id}")
    @ResponseBody
    public RestResponse<JobStatusResponse> getJob(@PathVariable String id, Principal principal) {
        Job job = jobManager.getJob(id);
        if (job == null) {
            throw new ResourceNotFoundException("Job not found: " + id);
        }
        User user = accessPolicyService.currentUser(principal);
        accessPolicyService.ensureOwnerOrAdmin(job.getUser(), user);
        return RestResponse.ok(toJobResponse(job));
    }

    @GetMapping("/api/status/{id}")
    @ResponseBody
    public RestResponse<JobStatusResponse> getStatusCompatibility(@PathVariable String id, Principal principal) {
        return getJob(id, principal);
    }

    @GetMapping("/api/jobs/{jobId}/files")
    @ResponseBody
    public RestResponse<List<JobFileResponse>> listFilesByJob(@PathVariable String jobId, Principal principal) {
        return listFilesCompatibility(jobId, principal);
    }

    @GetMapping("/api/files/{jobId}")
    @ResponseBody
    public RestResponse<List<JobFileResponse>> listFilesCompatibility(@PathVariable String jobId, Principal principal) {
        Job job = jobManager.getJob(jobId);
        if (job == null) {
            throw new ResourceNotFoundException("Job not found");
        }
        User user = accessPolicyService.currentUser(principal);
        accessPolicyService.ensureOwnerOrAdmin(job.getUser(), user);

        List<JobFileResponse> files = downloadArtifactService.listJobFiles(jobId).stream()
                .map(file -> dtoMapper.toJobFile(
                        file.get("name"),
                        file.get("path"),
                        file.get("contentType"),
                        Long.parseLong(file.getOrDefault("size", "0"))
                ))
                .toList();
        return RestResponse.ok(files);
    }

    @GetMapping("/files/{jobId}/{filename}")
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

    @PostMapping("/api/provider-credentials/{platform}/cookies")
    @ResponseBody
    public RestResponse<Void> uploadProviderCookie(@PathVariable String platform,
                                                   @RequestParam("file") MultipartFile file,
                                                   Principal principal) {
        User user = accessPolicyService.currentUser(principal);
        try {
            downloaderService.saveProviderCookie(file, user, parsePlatform(platform));
            return RestResponse.ok(null, "Táº£i lÃªn cookie thÃ nh cÃ´ng");
        } catch (Exception e) {
            throw new BusinessException("Lá»—i upload: " + e.getMessage());
        }
    }

    @DeleteMapping("/api/provider-credentials/{platform}/cookies")
    @ResponseBody
    public RestResponse<Void> deleteProviderCookie(@PathVariable String platform, Principal principal) {
        User user = accessPolicyService.currentUser(principal);
        downloaderService.deleteProviderCookie(user, parsePlatform(platform));
        return RestResponse.ok(null, "ÄÃ£ xoÃ¡ file cookie");
    }

    @GetMapping("/api/provider-credentials/status")
    @ResponseBody
    public RestResponse<Map<String, Boolean>> getProviderCredentialStatus(Principal principal) {
        User user = accessPolicyService.currentUser(principal);
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (Platform platform : Platform.values()) {
            result.put(platform.name(), downloaderService.hasProviderCookie(user, platform));
        }
        return RestResponse.ok(result);
    }

    @PostMapping("/api/upload-cookies")
    @ResponseBody
    public RestResponse<Void> uploadCookiesCompatibility(@RequestParam("file") MultipartFile file, Principal principal) {
        return uploadProviderCookie("YOUTUBE", file, principal);
    }

    @DeleteMapping("/api/cookies")
    @ResponseBody
    public RestResponse<Void> deleteCookiesCompatibility(Principal principal) {
        return deleteProviderCookie("YOUTUBE", principal);
    }

    @PostMapping("/api/telegram-chatid")
    @ResponseBody
    public RestResponse<Void> updateTelegramChatId(@RequestBody Map<String, String> body, Principal principal) {
        User user = accessPolicyService.currentUser(principal);
        String chatId = body.getOrDefault("chatId", "").trim();
        user.setTelegramChatId(chatId.isEmpty() ? null : chatId);
        userRepository.save(user);
        return RestResponse.ok(null, chatId.isEmpty()
                ? "ÄÃ£ xoÃ¡ Telegram Chat ID"
                : "ÄÃ£ lÆ°u Telegram Chat ID");
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

    private Platform parsePlatform(String value) {
        return Platform.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
