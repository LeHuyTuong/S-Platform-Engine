import { useCallback, useEffect, useState } from 'react';
import { listAdminUsers } from '../../../api/admin';
import type { AdminUser } from '../../../api/adminTypes';
import type { ApiClientError } from '../../../api/types';
import { toApiClientError } from '../../../api/types';

interface Options {
  enabled: boolean;
}

export function useAdminUsers({ enabled }: Options) {
  const [users, setUsers] = useState<AdminUser[]>([]);
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
      const response = await listAdminUsers();
      setUsers(response.data);
      setError(null);
    } catch (fetchError) {
      setError(toApiClientError(fetchError));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [enabled]);

  useEffect(() => {
    if (!enabled) {
      setLoading(false);
      return;
    }

    void load();
  }, [enabled, load]);

  return {
    users,
    loading,
    refreshing,
    error,
    refetch: () => load(true),
  };
}
