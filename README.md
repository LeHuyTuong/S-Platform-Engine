# S-Platform (Scalable Video Processing)

A distributed system demonstration for high-throughput video processing, built with **Spring Boot 3** and **Docker**.

## 🚀 Overview

This project serves as a Proof of Concept (PoC) for a scalable backend architecture capable of handling long-running asynchronous tasks. It demonstrates how to decouple job submission from execution using a Producer-Consumer pattern, ensuring system execute under heavy load.

Instead of processing tasks synchronously (which would block the main thread), this system offloads heavy lifting to background workers, managed by a robust queuing mechanism.

## 🏗 Architecture

The system is designed with a **Modular Monolith** approach, ready to be split into microservices:

1.  **API Layer (Controller)**: Handles HTTP requests and immediately returns a Job ID (Non-blocking).
2.  **Service Layer**: Business logic for validation and task coordination.
3.  **Job Manager**: Manages thread pools and execution context.
4.  **Worker Nodes**: Executes the actual video processing logic (wrapping `yt-dlp`).
5.  **Persistence**:
    *   **MySQL**: Stores structured job metadata and history.
    *   **Redis** (Optional): Cache layer for job status pooling (configured in `docker-compose`).

## ✨ Key Features

-   **Asynchronous Processing**: Non-blocking REST API for job submission.
-   **Rate Limiting & Throttling**: Configurable safeguards to respect external API limits (preventing 429 Too Many Requests).
-   **Fault Tolerance**:
    -   Automatic retries with exponential backoff.
    -   Graceful error handling for partial failures.
-   **Dockerized Environment**: One-command setup for App + Database + Cache.
-   **Security**:
    -   Input validation.
    -   Cookie-based authentication support (securely handled).

## 🛠 Tech Stack

-   **Language**: Java 17
-   **Framework**: Spring Boot 3.3.0
-   **Build Tool**: Maven
-   **Database**: MySQL 8.0
-   **Containerization**: Docker & Docker Compose
-   **External Tools**: `yt-dlp` (embedded binary for processing)

## ⚡ Getting Started

### Prerequisites
-   Java 17+
-   Docker & Docker Compose

### Running with Docker (Recommended)
```bash
docker-compose up -d --build
```
The application will be available at `http://localhost:8080`.

### Configuration
Adjust `src/main/resources/application.yml` for custom settings:
```yaml
app:
  downloader:
    output-dir: "downloads"
```

## ⚠️ Disclaimer
This project is for **educational and research purposes only**. It is designed to demonstrate system design principles such as asynchronous processing, rate limiting, and dockerization. The author is not responsible for any misuse of this software.
