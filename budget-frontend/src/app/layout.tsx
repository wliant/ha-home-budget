import type { Metadata } from 'next';
import { headers } from 'next/headers';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import theme from '../styles/theme';
import AppShell from '../components/navigation/AppShell';
import { IngressProvider } from '../contexts/IngressContext';

export const metadata: Metadata = {
  title: 'Home Budget Tracker',
  description: 'Household budget and expense tracking system',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const headersList = headers();
  const ingressPath = headersList.get('x-ingress-path') || '';

  return (
    <html lang="en">
      <head>
        <script
          dangerouslySetInnerHTML={{
            __html: `
window.__INGRESS_PATH__ = ${JSON.stringify(ingressPath)};
if (window.__INGRESS_PATH__) {
  var _origFetch = window.fetch;
  window.fetch = function(input, init) {
    if (typeof input === 'string' && input.startsWith('/') && !input.startsWith(window.__INGRESS_PATH__)) {
      input = window.__INGRESS_PATH__ + input;
    }
    return _origFetch.call(this, input, init);
  };
}
`,
          }}
        />
      </head>
      <body>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <IngressProvider>
            <AppShell>{children}</AppShell>
          </IngressProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
