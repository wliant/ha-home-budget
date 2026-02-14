'use client';

import { usePathname } from 'next/navigation';

/**
 * Returns the app-relative pathname with the HA ingress prefix stripped.
 * Falls back to usePathname() when not running under ingress.
 */
export function useAppPathname(): string {
  const pathname = usePathname();

  if (typeof window !== 'undefined' && window.__INGRESS_PATH__ && pathname.startsWith(window.__INGRESS_PATH__)) {
    const stripped = pathname.slice(window.__INGRESS_PATH__.length);
    return stripped === '' || stripped === '/' ? '/' : stripped;
  }

  return pathname;
}
