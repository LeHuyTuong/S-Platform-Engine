import { useCallback, useEffect, useState } from 'react';
import { listJobs } from '../../../api/jobs';
import type { Job } from '../../../api/downloaderTypes';
import type { ApiMeta, ApiClientError } from '../../../api/types';
import { toApiClientError } from '../../../api/types';

interface UseJobsOptions {
  page?: number;
  size?: number;
}

export function useJobs({ page = 0, size = 20 }: UseJobsOptions = {}) {
  const [items, setItems] = useState<Job[]>([]);
  const [meta, setMeta] = useState<ApiMeta | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<ApiClientError | null>(null);

  useEffect(() => {
    let isCancelled = false;

    async function loadInitialJobs() {
      try {
        const response = await listJobs({ page, size });

        if (isCancelled) {
          return;
        }

        setItems(response.data);
        setMeta(response.meta);
        setError(null);
      } catch (fetchError) {
        if (!isCancelled) {
          setError(toApiClientError(fetchError));
        }
      } finally {
        if (!isCancelled) {
          setLoading(false);
        }
      }
    }

    void loadInitialJobs();

    return () => {
      isCancelled = true;
    };
  }, [page, size]);

  const refetch = useCallback(
    async (background = false) => {
      if (background) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }

      try {
        const response = await listJobs({ page, size });
        setItems(response.data);
        setMeta(response.meta);
        setError(null);
      } catch (fetchError) {
        setError(toApiClientError(fetchError));
      } finally {
        if (background) {
          setRefreshing(false);
        } else {
          setLoading(false);
        }
      }
    },
    [page, size],
  );

  const upsertJob = useCallback(
    (job: Job) => {
      setItems((previous) => {
        const index = previous.findIndex((item) => item.id === job.id);

        if (index === -1) {
          return [job, ...previous].slice(0, size);
        }

        const next = [...previous];
        next[index] = job;
        return next;
      });
    },
    [size],
  );

  const prependJob = useCallback(
    (job: Job) => {
      setItems((previous) => [job, ...previous.filter((item) => item.id !== job.id)].slice(0, size));
    },
    [size],
  );

  return {
    items,
    meta,
    loading,
    refreshing,
    error,
    refetch,
    upsertJob,
    prependJob,
  };
}
