import { useCallback, useEffect, useState } from 'react';
import { listSourceRequests } from '../../../api/sourceRequests';
import type { SourceRequest } from '../../../api/downloaderTypes';
import type { ApiMeta, ApiClientError } from '../../../api/types';
import { toApiClientError } from '../../../api/types';

interface UseSourceRequestsOptions {
  page?: number;
  size?: number;
  autoRefreshMs?: number;
}

const ACTIVE_SOURCE_REQUEST_STATES = new Set(['ACCEPTED', 'RESOLVING']);

export function useSourceRequests({
  page = 0,
  size = 10,
  autoRefreshMs = 5_000,
}: UseSourceRequestsOptions = {}) {
  const [items, setItems] = useState<SourceRequest[]>([]);
  const [meta, setMeta] = useState<ApiMeta | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<ApiClientError | null>(null);

  useEffect(() => {
    let isCancelled = false;

    async function loadInitialSourceRequests() {
      try {
        const response = await listSourceRequests({ page, size });

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

    void loadInitialSourceRequests();

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
        const response = await listSourceRequests({ page, size });
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

  const hasPending = items.some((item) => ACTIVE_SOURCE_REQUEST_STATES.has(item.state));

  useEffect(() => {
    if (!hasPending) {
      return;
    }

    const intervalId = window.setInterval(() => {
      void refetch(true);
    }, autoRefreshMs);

    return () => window.clearInterval(intervalId);
  }, [autoRefreshMs, hasPending, refetch]);

  const prependSourceRequest = useCallback(
    (sourceRequest: SourceRequest) => {
      setItems((previous) => [sourceRequest, ...previous.filter((item) => item.id !== sourceRequest.id)].slice(0, size));
    },
    [size],
  );

  const upsertSourceRequest = useCallback(
    (sourceRequest: SourceRequest) => {
      setItems((previous) => {
        const index = previous.findIndex((item) => item.id === sourceRequest.id);

        if (index === -1) {
          return [sourceRequest, ...previous].slice(0, size);
        }

        const next = [...previous];
        next[index] = sourceRequest;
        return next;
      });
    },
    [size],
  );

  return {
    items,
    meta,
    loading,
    refreshing,
    error,
    hasPending,
    refetch,
    prependSourceRequest,
    upsertSourceRequest,
  };
}
