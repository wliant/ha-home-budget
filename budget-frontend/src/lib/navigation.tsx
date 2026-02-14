'use client';

import { forwardRef } from 'react';
import NextLink from 'next/link';
import type { LinkProps } from 'next/link';
import { useRouter } from 'next/navigation';
import { useIngressPath } from '../contexts/IngressContext';

function prefixHref(href: string, ingressPath: string): string {
  if (ingressPath && href.startsWith('/') && !href.startsWith(ingressPath)) {
    return ingressPath.replace(/\/$/, '') + href;
  }
  return href;
}

/**
 * Ingress-aware Link component.
 * Wraps next/link and prepends the HA ingress path to hrefs.
 */
const Link = forwardRef<HTMLAnchorElement, LinkProps & { className?: string; children?: React.ReactNode; style?: React.CSSProperties; [key: string]: any }>(
  function IngressLink({ href, ...props }, ref) {
    const ingressPath = useIngressPath();
    const resolved = typeof href === 'string' ? prefixHref(href, ingressPath) : href;
    return <NextLink ref={ref} href={resolved} {...props} />;
  }
);

Link.displayName = 'IngressLink';

export default Link;

/**
 * Ingress-aware useRouter hook.
 * Wraps next/navigation useRouter and prepends ingress path to push/replace.
 */
export function useIngressRouter() {
  const router = useRouter();
  const ingressPath = useIngressPath();

  return {
    push: (href: string, options?: any) => router.push(prefixHref(href, ingressPath), options),
    replace: (href: string, options?: any) => router.replace(prefixHref(href, ingressPath), options),
    refresh: () => router.refresh(),
    back: () => router.back(),
    forward: () => router.forward(),
    prefetch: (href: string) => router.prefetch(prefixHref(href, ingressPath)),
  };
}
