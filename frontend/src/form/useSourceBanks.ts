import { useEffect, useState } from 'react';
import { fetchSourceBanks } from '../api/transactions';

export interface SourceBanks {
  banks: string[];
  failed: boolean;
}

/** Loads the banks transaction-service accepts, once, when the form first appears. */
export function useSourceBanks(): SourceBanks {
  const [sourceBanks, setSourceBanks] = useState<SourceBanks>({ banks: [], failed: false });
  useEffect(() => {
    fetchSourceBanks().then(
      (banks) => setSourceBanks({ banks, failed: false }),
      () => setSourceBanks({ banks: [], failed: true }),
    );
  }, []);
  return sourceBanks;
}
