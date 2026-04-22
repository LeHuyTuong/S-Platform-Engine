import { useCallback, useEffect, useState } from 'react';
import { getAdminSettings } from '../../../api/admin';
import type { AdminSettings } from '../../../api/adminTypes';
import type { ApiClientError } from '../../../api/types';
import { toApiClientError } from '../../../api/types';

interface Options {
  enabled: boolean;
}

export function useAdminSettings({ enabled }: Options) {
  const [settings, setSettings] = useState<AdminSettings | null>(null);
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
      const response = await getAdminSettings();
      setSettings(response.data);
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
    settings,
    loading,
    refreshing,
    error,
    refetch: () => load(true),
  };
}
