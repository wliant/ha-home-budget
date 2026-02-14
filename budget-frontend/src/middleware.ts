import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function middleware(request: NextRequest) {
  const response = NextResponse.next();
  const ingressPath = request.headers.get('x-ingress-path');

  if (ingressPath) {
    response.cookies.set('__ingress_path', ingressPath, {
      path: '/',
      sameSite: 'lax',
    });
  }

  return response;
}

export const config = {
  matcher: '/:path*',
};
