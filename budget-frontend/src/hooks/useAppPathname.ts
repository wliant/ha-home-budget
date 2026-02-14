'use client';

import { usePathname } from 'next/navigation';
import { useIngressPath } from '../contexts/IngressContext';

/**
 * Returns the app-relative pathname with the HA ingress prefix stripped.
 * Falls back to usePathname() when not running under ingress.
 */
export function useAppPathname(): string {
  const pathname = usePathname();
  const ingressPath = useIngressPath();

  if (ingressPath && pathname.startsWith(ingressPath.replace(/\/$/, ''))) {
    const prefix = ingressPath.replace(/\/$/, '');
    const stripped = pathname.slice(prefix.length);
    return stripped === '' ? '/' : stripped;
  }

  return pathname;
}
