import { useCallback, useEffect, useState } from 'react';
import { getAuthSession } from '../../../api/auth';
import type { AuthSession } from '../../../api/downloaderTypes';
import type { ApiClientError } from '../../../api/types';
import { toApiClientError } from '../../../api/types';

export function useAuthSession() {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<ApiClientError | null>(null);

  useEffect(() => {
    let isCancelled = false;

    async function loadSession() {
      try {
        const response = await getAuthSession();

        if (isCancelled) {
          return;
        }

        setSession(response.data);
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

    void loadSession();

    return () => {
      isCancelled = true;
    };
  }, []);

  const refetch = useCallback(async () => {
    setLoading(true);

    try {
      const response = await getAuthSession();
      setSession(response.data);
      setError(null);
    } catch (fetchError) {
      setError(toApiClientError(fetchError));
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    session,
    loading,
    error,
    refetch,
  };
}
