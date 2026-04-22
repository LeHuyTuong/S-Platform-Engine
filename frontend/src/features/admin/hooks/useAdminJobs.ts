import { useCallback, useEffect, useState } from 'react';
import { listAdminJobs } from '../../../api/admin';
import type { AdminJob, AdminJobListParams } from '../../../api/adminTypes';
import type { ApiClientError, ApiMeta } from '../../../api/types';
import { toApiClientError } from '../../../api/types';

interface Options extends AdminJobListParams {
  enabled: boolean;
}

export function useAdminJobs({ enabled, page = 0, size = 20, state = '', status = '', platform = '' }: Options) {
  const [jobs, setJobs] = useState<AdminJob[]>([]);
  const [meta, setMeta] = useState<ApiMeta | null>(null);
  const [loading, setLoading] = useState(enabled);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<ApiClientError | null>(null);

  const load = useCallback(async (isRefreshing = false) => {
    if (!enabled) {
      setLoading(false);
      setRefreshing(false);
      return;
    }

    if (isRefreshing) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }

    try {
      const response = await listAdminJobs({ page, size, state, status, platform });
      setJobs(response.data);
      setMeta(response.meta);
      setError(null);
    } catch (fetchError) {
      setError(toApiClientError(fetchError));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [enabled, page, platform, size, state, status]);

  useEffect(() => {
    if (!enabled) {
      setLoading(false);
      return;
    }

    void load();
  }, [enabled, load]);

  return {
    jobs,
    meta,
    loading,
    refreshing,
    error,
    refetch: () => load(true),
  };
}
