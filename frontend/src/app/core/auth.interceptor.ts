import { HttpErrorResponse, HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { catchError, switchMap, throwError } from "rxjs";
import { AuthService } from "./auth.service";

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const isAuthEndpoint = /\/api\/v1\/auth\/(login|register|refresh|logout)$/.test(request.url);
  const requestWithCredentials = request.clone({ withCredentials: true });

  if (isAuthEndpoint) {
    return next(requestWithCredentials);
  }

  const token = auth.token;
  const authorizedRequest = token
    ? requestWithCredentials.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      })
    : requestWithCredentials;

  return next(authorizedRequest).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || !token) {
        return throwError(() => error);
      }

      return auth.refreshAccessToken().pipe(
        switchMap((response) => {
          return next(
            requestWithCredentials.clone({
              setHeaders: {
                Authorization: `Bearer ${response.accessToken}`
              }
            })
          );
        }),
        catchError((refreshError) => {
          auth.expireSession();
          return throwError(() => refreshError);
        })
      );
    })
  );
};
