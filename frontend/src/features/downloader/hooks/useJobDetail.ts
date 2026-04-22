import { useCallback, useEffect, useState } from 'react';
import { getJob, listJobFiles } from '../../../api/jobs';
import type { Job, JobFile } from '../../../api/downloaderTypes';
import type { ApiClientError } from '../../../api/types';
import { toApiClientError } from '../../../api/types';

interface UseJobDetailOptions {
  jobId: string | null;
  pollMs?: number;
}

const POLLING_STATES = new Set([
  'ACCEPTED',
  'RESOLVING',
  'QUEUED',
  'RUNNING',
  'POST_PROCESSING',
  'RETRY_WAIT',
]);

export function useJobDetail({ jobId, pollMs = 1_500 }: UseJobDetailOptions) {
  const [storedJob, setStoredJob] = useState<Job | null>(null);
  const [storedFiles, setStoredFiles] = useState<JobFile[]>([]);
  const [refreshing, setRefreshing] = useState(false);
  const [storedError, setStoredError] = useState<ApiClientError | null>(null);
  const [storedFilesError, setStoredFilesError] = useState<ApiClientError | null>(null);
  const [loadedFilesForJobId, setLoadedFilesForJobId] = useState<string | null>(null);

  useEffect(() => {
    if (!jobId) {
      return;
    }

    const currentJobId = jobId;
    let isCancelled = false;

    async function loadJob() {
      try {
        const response = await getJob(currentJobId);

        if (isCancelled) {
          return;
        }

        setStoredJob(response.data);
        setStoredError(null);
      } catch (fetchError) {
        if (!isCancelled) {
          setStoredError(toApiClientError(fetchError));
        }
      }
    }

    void loadJob();

    return () => {
      isCancelled = true;
    };
  }, [jobId]);

  const job = storedJob?.id === jobId ? storedJob : null;
  const error = jobId && storedJob?.id !== jobId ? null : storedError;
  const files = job && loadedFilesForJobId === job.id ? storedFiles : [];
  const filesError = job && (loadedFilesForJobId === job.id || storedFilesError != null) ? storedFilesError : null;
  const loading = Boolean(jobId) && job == null && error == null;
  const isPolling = Boolean(job && POLLING_STATES.has(job.state));
  const filesLoading = Boolean(job && job.state === 'COMPLETED' && loadedFilesForJobId !== job.id && !filesError);

  const refetch = useCallback(
    async (background = false) => {
      if (!jobId) {
        return;
      }

      if (background) {
        setRefreshing(true);
      }

      try {
        const response = await getJob(jobId);
        setStoredJob(response.data);
        setStoredError(null);
      } catch (fetchError) {
        setStoredError(toApiClientError(fetchError));
      } finally {
        if (background) {
          setRefreshing(false);
        }
      }
    },
    [jobId],
  );

  useEffect(() => {
    if (!jobId || !isPolling) {
      return;
    }

    const intervalId = window.setInterval(() => {
      void refetch(true);
    }, pollMs);

    return () => window.clearInterval(intervalId);
  }, [isPolling, jobId, pollMs, refetch]);

  useEffect(() => {
    if (!job || job.state !== 'COMPLETED' || loadedFilesForJobId === job.id) {
      return;
    }

    const completedJob = job;
    let isCancelled = false;

    async function loadFiles() {
      try {
        const response = await listJobFiles(completedJob.id);

        if (isCancelled) {
          return;
        }

        setStoredFiles(response.data);
        setLoadedFilesForJobId(completedJob.id);
        setStoredFilesError(null);
      } catch (fetchError) {
        if (!isCancelled) {
          setStoredFilesError(toApiClientError(fetchError));
        }
      }
    }

    void loadFiles();

    return () => {
      isCancelled = true;
    };
  }, [job, loadedFilesForJobId]);

  const refetchFiles = useCallback(async () => {
    if (!jobId) {
      return;
    }

    setLoadedFilesForJobId(null);
    setStoredFilesError(null);

    try {
      const response = await listJobFiles(jobId);
      setStoredFiles(response.data);
      setLoadedFilesForJobId(jobId);
    } catch (fetchError) {
      setStoredFilesError(toApiClientError(fetchError));
    }
  }, [jobId]);

  return {
    job,
    files,
    loading,
    refreshing,
    error,
    filesLoading,
    filesError,
    isPolling,
    refetch,
    refetchFiles,
  };
}
