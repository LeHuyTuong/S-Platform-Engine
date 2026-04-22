# S-Platform E2E CLI Test Script
$BaseUrl = "http://localhost:8080"

# 1. Get Login Page & CSRF Token
Write-Host ">>> Fetching login page..."
$LoginResp = Invoke-WebRequest -Uri "$BaseUrl/login" -SessionVariable sess -UseBasicParsing
$csrfToken = $LoginResp.InputFields | Where-Object { $_.name -eq "_csrf" } | Select-Object -ExpandProperty value

if (-not $csrfToken) {
    Write-Error "Could not find CSRF token on login page."
    exit 1
}
Write-Host ">>> CSRF Token: $csrfToken"

# 2. Login
Write-Host ">>> Logging in as user@test.com..."
$LoginBody = @{
    username = "user@test.com"
    password = "user"
    _csrf = $csrfToken
}
$AuthResp = Invoke-WebRequest -Uri "$BaseUrl/login" -Method Post -Body $LoginBody -WebSession $sess -UseBasicParsing -ErrorAction SilentlyContinue

if ($AuthResp.StatusCode -eq 302 -or $AuthResp.StatusCode -eq 200) {
    Write-Host ">>> Login successful (or redirected)."
} else {
    Write-Error "Login failed with status $($AuthResp.StatusCode)"
    exit 1
}

# 3. Submit a Job
Write-Host ">>> Submitting download job..."
$JobBody = @{
    sourceUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    downloadType = "VIDEO"
    format = "mp4"
    quality = "best"
    platform = "AUTO"
    sourceType = "AUTO"
} | ConvertTo-Json

# Get CSRF for API (Header X-CSRF-TOKEN)
$DownloaderPage = Invoke-WebRequest -Uri "$BaseUrl/downloader" -WebSession $sess -UseBasicParsing
$apiCsrf = $DownloaderPage.Content -match 'name="_csrf" content="([^"]+)"' | Out-Null
$apiCsrf = $Matches[1]

$Headers = @{
    "Content-Type" = "application/json"
    "X-CSRF-TOKEN" = $apiCsrf
}

$SubmitResp = Invoke-WebRequest -Uri "$BaseUrl/downloader/api/source-requests" -Method Post -Body $JobBody -Headers $Headers -WebSession $sess -UseBasicParsing
$JobData = $SubmitResp.Content | ConvertFrom-Json

if ($JobData.success -eq $true) {
    $JobId = $JobData.data.id
    Write-Host ">>> Job submitted successfully! ID: $JobId"
} else {
    Write-Error "Job submission failed: $($JobData.message)"
    exit 1
}

# 4. Poll Status
Write-Host ">>> Polling source request status (5 iterations)..."
for ($i = 1; $i -le 5; $i++) {
    Start-Sleep -Seconds 3
    $StatusResp = Invoke-WebRequest -Uri "$BaseUrl/downloader/api/source-requests/$JobId" -Headers $Headers -WebSession $sess -UseBasicParsing
    $StatusData = $StatusResp.Content | ConvertFrom-Json
    $State = $StatusData.data.state
    $JobCount = $StatusData.data.jobs.Count
    Write-Host ">>> [$i] State: $State | Child Jobs: $JobCount"
    
    if ($State -eq "COMPLETED" -or $State -eq "PROCESSED") {
        Write-Host ">>> Source Request PROCESSED successfully!"
        break
    }
    if ($State -eq "FAILED") {
        Write-Error "Source Request FAILED: $($StatusData.data.errorMessage)"
        break
    }
}

Write-Host ">>> E2E Test Finished."
