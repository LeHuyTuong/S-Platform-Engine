create table users (
    id bigint not null auto_increment,
    email varchar(255) not null,
    password_hash varchar(255) not null,
    role varchar(32) not null,
    enabled boolean not null,
    telegram_chat_id varchar(255),
    created_at timestamp not null,
    updated_at timestamp,
    primary key (id),
    constraint uk_users_email unique (email)
);

create table source_requests (
    id varchar(36) not null,
    user_id bigint not null,
    platform varchar(32) not null,
    source_type varchar(32) not null,
    state varchar(32) not null,
    source_url varchar(1000) not null,
    normalized_url varchar(1000),
    requested_download_type varchar(32),
    requested_quality varchar(64),
    requested_format varchar(64),
    proxy_ref varchar(255),
    proxy varchar(500),
    start_time varchar(32),
    end_time varchar(32),
    clean_metadata boolean not null,
    write_thumbnail boolean not null,
    watermark_text varchar(255),
    title_template varchar(255),
    error_message varchar(2000),
    blocked_reason varchar(1000),
    resolved_count integer,
    created_at timestamp not null,
    updated_at timestamp,
    primary key (id),
    constraint fk_source_requests_user foreign key (user_id) references users(id)
);

create index idx_source_requests_user_created on source_requests (user_id, created_at desc);
create index idx_source_requests_state on source_requests (state);

create table jobs (
    id varchar(36) not null,
    user_id bigint,
    source_request_id varchar(36),
    url varchar(1000) not null,
    status varchar(32),
    state varchar(32),
    platform varchar(32),
    source_type varchar(32),
    failure_category varchar(64),
    output_filename varchar(500),
    playlist_title varchar(500),
    video_title varchar(500),
    total_items integer,
    current_item integer,
    download_type varchar(32),
    quality varchar(64),
    format varchar(64),
    requested_variant varchar(128),
    external_item_id varchar(255),
    proxy varchar(500),
    proxy_ref varchar(255),
    start_time varchar(32),
    end_time varchar(32),
    clean_metadata boolean not null,
    write_thumbnail boolean not null,
    watermark_text varchar(255),
    title_template varchar(255),
    error_message varchar(2000),
    author_name varchar(255),
    caption_text text,
    published_at timestamp,
    duration_seconds bigint,
    thumbnail_url varchar(1000),
    availability varchar(128),
    attempt_count integer not null,
    max_attempts integer not null,
    next_attempt_at timestamp,
    lease_owner varchar(255),
    lease_expires_at timestamp,
    queued_at timestamp,
    started_at timestamp,
    finished_at timestamp,
    download_path varchar(1000),
    created_at timestamp not null,
    updated_at timestamp,
    primary key (id),
    constraint fk_jobs_user foreign key (user_id) references users(id),
    constraint fk_jobs_source_request foreign key (source_request_id) references source_requests(id)
);

create index idx_jobs_user_created on jobs (user_id, created_at desc);
create index idx_jobs_source_request on jobs (source_request_id);
create index idx_jobs_state_next_attempt on jobs (state, next_attempt_at);
create index idx_jobs_lease on jobs (lease_expires_at);
create index idx_jobs_external_item on jobs (user_id, platform, external_item_id, requested_variant);

create table download_attempts (
    id bigint not null auto_increment,
    job_id varchar(36) not null,
    attempt_number integer not null,
    started_at timestamp not null,
    finished_at timestamp,
    success boolean not null,
    exit_code integer,
    failure_category varchar(64),
    error_message varchar(2000),
    created_at timestamp not null,
    updated_at timestamp,
    primary key (id),
    constraint fk_attempts_job foreign key (job_id) references jobs(id)
);

create index idx_attempts_job on download_attempts (job_id, attempt_number desc);

create table stored_assets (
    id bigint not null auto_increment,
    job_id varchar(36) not null,
    asset_type varchar(64) not null,
    file_name varchar(500) not null,
    relative_path varchar(1000) not null,
    content_type varchar(255),
    size_bytes bigint not null,
    checksum_sha256 varchar(128),
    created_at timestamp not null,
    updated_at timestamp,
    primary key (id),
    constraint fk_assets_job foreign key (job_id) references jobs(id)
);

create index idx_assets_job on stored_assets (job_id);

create table provider_credentials (
    id bigint not null auto_increment,
    user_id bigint not null,
    platform varchar(32) not null,
    credential_type varchar(32) not null,
    encrypted_payload text not null,
    iv varchar(255) not null,
    file_name varchar(255),
    created_at timestamp not null,
    updated_at timestamp,
    primary key (id),
    constraint fk_credentials_user foreign key (user_id) references users(id),
    constraint uk_credentials_user_platform_type unique (user_id, platform, credential_type)
);

create table user_connection_settings (
    id bigint not null auto_increment,
    user_id bigint not null,
    encrypted_telegram_bot_token text,
    telegram_bot_token_iv varchar(255),
    telegram_chat_id varchar(255),
    encrypted_google_drive_service_account_json text,
    google_drive_service_account_json_iv varchar(255),
    google_drive_folder_id varchar(255),
    base_url varchar(512),
    created_at timestamp not null,
    updated_at timestamp,
    primary key (id),
    constraint fk_connection_settings_user foreign key (user_id) references users(id),
    constraint uk_connection_settings_user unique (user_id)
);

create table job_events (
    id bigint not null auto_increment,
    job_id varchar(36) not null,
    sequence_no bigint not null,
    level varchar(16) not null,
    message text not null,
    created_at timestamp not null,
    updated_at timestamp,
    primary key (id),
    constraint fk_job_events_job foreign key (job_id) references jobs(id)
);

create index idx_job_events_job_seq on job_events (job_id, sequence_no desc);

create table outbox_events (
    id varchar(36) not null,
    aggregate_type varchar(64) not null,
    aggregate_id varchar(64) not null,
    event_type varchar(64) not null,
    payload text not null,
    status varchar(32) not null,
    attempts integer not null,
    available_at timestamp not null,
    published_at timestamp,
    processed_at timestamp,
    last_error varchar(2000),
    stream_message_id varchar(128),
    created_at timestamp not null,
    updated_at timestamp,
    primary key (id)
);

create index idx_outbox_status_available on outbox_events (status, available_at);
