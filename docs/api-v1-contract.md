# API v1 Contract

This document describes the backend contract that the frontend can rely on for the current REST API surface.

## Base URL

- Local: `http://localhost:8080`
- Protected API root: `/api/v1`

## Response Envelope

Every JSON response under `/api/v1/**` uses the same wrapper:

```json
{
  "success": true,
  "message": "Success",
  "data": {},
  "meta": {
    "page": 0,
    "size": 20,
    "totalItems": 1,
    "totalPages": 1,
    "hasNext": false,
    "hasPrevious": false
  },
  "error": null,
  "requestId": "b7c7b4a8-7f2c-4cbf-9f49-4d2dce6f1a1a",
  "path": "/api/v1/jobs",
  "timestamp": "2026-04-21T23:00:00"
}
```

Notes:

- `meta` is only present for paginated list endpoints.
- `error` is present only on failures.
- `requestId` is mirrored in the `X-Request-Id` response header.
- Binary download endpoints return file content directly and do not use the JSON envelope.

## Auth Flow

The API uses session authentication plus CSRF protection.

Frontend flow:

1. `GET /api/v1/auth/csrf`
2. Read `data.token` and `data.headerName`
3. `POST /api/v1/auth/login` with the CSRF header and `credentials: "include"`
4. Reuse the session cookie for all later `/api/v1/**` requests

Important fetch settings:

- Always send `credentials: "include"`
- For mutating requests (`POST`, `PUT`, `DELETE`), send the CSRF header returned by `/api/v1/auth/csrf`

## Pagination Contract

The following endpoints are paginated:

- `GET /api/v1/source-requests`
- `GET /api/v1/jobs`
- `GET /api/v1/admin/jobs`

Query parameters:

- `page`: zero-based, must be `>= 0`
- `size`: must be between `1` and `100`

Invalid pagination returns HTTP `400` with the standard error envelope.

## Main Endpoints

### Auth

- `GET /api/v1/auth/csrf`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`

`POST /api/v1/auth/login` body:

```json
{
  "email": "user@test.com",
  "password": "user"
}
```

`GET /api/v1/auth/me` data shape:

```json
{
  "authenticated": true,
  "email": "user@test.com",
  "role": "USER",
  "enabled": true,
  "dailyQuota": 10,
  "jobsToday": 0,
  "canUseProxy": false,
  "canManageRuntimeSettings": false
}
```

### Source Requests

- `POST /api/v1/source-requests`
- `GET /api/v1/source-requests`
- `GET /api/v1/source-requests/{id}`

Submission body:

```json
{
  "sourceUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
  "platform": "AUTO",
  "sourceType": "AUTO",
  "downloadType": "VIDEO",
  "quality": "best",
  "format": "mp4",
  "writeThumbnail": true,
  "cleanMetadata": false,
  "startTime": null,
  "endTime": null,
  "proxyRef": null,
  "proxy": null,
  "titleTemplate": null,
  "watermarkText": null
}
```

Notes:

- `platform` may be omitted, left blank, or sent as `AUTO`. In these cases the backend detects the provider from the URL.
- `sourceType` may be omitted, left blank, or sent as `AUTO`. In these cases the backend infers `DIRECT_URL`, `PLAYLIST`, or `PROFILE` from the URL pattern.
- Audio requests should still send `quality: "best"` even when the frontend hides the quality picker.

`SourceRequestResponse`:

```json
{
  "id": "src_123",
  "platform": "YOUTUBE",
  "sourceType": "DIRECT_URL",
  "state": "ACCEPTED",
  "sourceUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
  "resolvedCount": 1,
  "errorMessage": null,
  "blockedReason": null,
  "createdAt": "2026-04-21T23:00:00",
  "jobs": []
}
```

### Jobs

- `GET /api/v1/jobs`
- `GET /api/v1/jobs/{id}`
- `GET /api/v1/jobs/{id}/logs`
- `GET /api/v1/jobs/{jobId}/files`
- `GET /api/v1/jobs/{jobId}/files/{filename}`

List filters:

- `state`: `ACCEPTED`, `RESOLVING`, `QUEUED`, `RUNNING`, `POST_PROCESSING`, `COMPLETED`, `FAILED`, `RETRY_WAIT`, `BLOCKED`
- `status`: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`
- `platform`: `YOUTUBE`, `TIKTOK`, `INSTAGRAM`, `FACEBOOK`

`JobStatusResponse`:

```json
{
  "id": "job_123",
  "sourceRequestId": "src_123",
  "status": "RUNNING",
  "state": "RUNNING",
  "platform": "YOUTUBE",
  "sourceType": "DIRECT_URL",
  "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
  "videoTitle": "Example video",
  "playlistTitle": null,
  "totalItems": 1,
  "currentItem": 1,
  "downloadType": "VIDEO",
  "quality": "best",
  "format": "mp4",
  "errorMessage": null,
  "downloadSpeed": "1.2MiB/s",
  "eta": "00:12",
  "progressPercent": 42.5,
  "createdAt": "2026-04-21T23:00:00",
  "logs": []
}
```

`JobFileResponse`:

```json
{
  "name": "video #1 100%.mp4",
  "path": null,
  "downloadUrl": "/api/v1/jobs/job_123/files/video%20%231%20100%25.mp4",
  "contentType": "video/mp4",
  "type": "video",
  "size": 128
}
```

## Runtime Settings

Admin and publisher only:

- `GET /api/v1/runtime-settings`
- `PUT /api/v1/runtime-settings`
- `DELETE /api/v1/runtime-settings`

`GET /api/v1/runtime-settings` data shape:

```json
{
  "hasSettings": false,
  "hasTelegramToken": false,
  "hasTelegramChatId": false,
  "hasGoogleDriveServiceAccount": false,
  "hasGoogleDriveFolderId": false,
  "hasBaseUrl": false
}
```

## Provider Credential Endpoints

- `GET /api/v1/provider-credentials/status`
- `POST /api/v1/provider-credentials/{platform}/cookies`
- `DELETE /api/v1/provider-credentials/{platform}/cookies`

The upload endpoint expects `multipart/form-data` with a `file` field.

## Admin API

Admin only:

- `GET /api/v1/admin/dashboard`
- `GET /api/v1/admin/jobs`
- `GET /api/v1/admin/users`
- `GET /api/v1/admin/settings`
- `PUT /api/v1/admin/settings`
- `POST /api/v1/admin/jobs/{id}/resubmit`
- `POST /api/v1/admin/jobs/backfill-titles`
