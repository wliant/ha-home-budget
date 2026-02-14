'use client';

import React, { createContext, useContext, useState, useEffect } from 'react';

declare global {
  interface Window {
    __INGRESS_PATH__?: string;
  }
}

interface IngressContextType {
  ingressPath: string;
}

const IngressContext = createContext<IngressContextType>({ ingressPath: '' });

export function IngressProvider({ children }: { children: React.ReactNode }) {
  const [ingressPath, setIngressPath] = useState('');

  useEffect(() => {
    if (typeof window !== 'undefined' && window.__INGRESS_PATH__) {
      setIngressPath(window.__INGRESS_PATH__);
    }
  }, []);

  return (
    <IngressContext.Provider value={{ ingressPath }}>
      {children}
    </IngressContext.Provider>
  );
}

export function useIngressPath(): string {
  const context = useContext(IngressContext);
  return context.ingressPath;
}

export function getIngressApiUrl(path: string): string {
  if (typeof window !== 'undefined' && window.__INGRESS_PATH__) {
    return window.__INGRESS_PATH__ + path;
  }
  return path;
}
