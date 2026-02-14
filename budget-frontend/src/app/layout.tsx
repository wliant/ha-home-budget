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
window.__INGRESS_PATH__ = ${JSON.stringify(ingressPath).replace(/</g, '\\u003c')};
if (window.__INGRESS_PATH__) {
  var _ip = window.__INGRESS_PATH__;
  function _pfx(u) {
    return (typeof u === 'string' && u.startsWith('/') && !u.startsWith(_ip)) ? _ip + u : u;
  }
  var _f = window.fetch;
  window.fetch = function(i, o) { return _f.call(this, (typeof i === 'string') ? _pfx(i) : i, o); };
  var _ps = history.pushState.bind(history);
  var _rs = history.replaceState.bind(history);
  history.pushState = function(s, t, u) { return _ps(s, t, _pfx(u)); };
  history.replaceState = function(s, t, u) { return _rs(s, t, _pfx(u)); };
  document.addEventListener('click', function(e) {
    var a = e.target.closest ? e.target.closest('a[href]') : null;
    if (!a) return;
    var h = a.getAttribute('href');
    if (h && h.startsWith('/') && !h.startsWith(_ip)) {
      a.setAttribute('href', _ip + h);
    }
  }, true);
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
