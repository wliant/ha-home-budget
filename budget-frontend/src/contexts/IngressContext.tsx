'use client';

import React, { createContext, useContext } from 'react';

declare global {
  interface Window {
    __INGRESS_PATH__?: string;
  }
}

interface IngressContextType {
  ingressPath: string;
}

const IngressContext = createContext<IngressContextType>({ ingressPath: '' });

export function IngressProvider({ children, ingressPath = '' }: { children: React.ReactNode; ingressPath?: string }) {
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
