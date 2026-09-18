import { Injectable } from "@angular/core";
import { Router } from "@angular/router";
import { BehaviorSubject, Observable, finalize, shareReplay, tap } from "rxjs";
import { ApiService } from "./api.service";
import { LoginResponse } from "../models";

@Injectable({ providedIn: "root" })
export class AuthService {
  private readonly subject = new BehaviorSubject<LoginResponse | null>(this.readStoredUser());
  private refreshInFlight$: Observable<LoginResponse> | null = null;
  readonly user$ = this.subject.asObservable();

  constructor(
    private readonly api: ApiService,
    private readonly router: Router
  ) {}

  get user(): LoginResponse | null {
    return this.subject.value;
  }

  get token(): string | null {
    return this.user?.accessToken ?? null;
  }

  login(request: {
    tenantSlug: string;
    email: string;
    password: string;
  }) {
    return this.api.login(request).pipe(
      tap((response) => this.storeUser(response))
    );
  }

  refreshAccessToken(): Observable<LoginResponse> {
    if (this.refreshInFlight$) {
      return this.refreshInFlight$;
    }

    this.refreshInFlight$ = this.api.refresh().pipe(
      tap((response) => this.storeUser(response)),
      finalize(() => {
        this.refreshInFlight$ = null;
      }),
      shareReplay(1)
    );

    return this.refreshInFlight$;
  }

  logout(): void {
    this.api.logout().subscribe({
      next: () => this.finishLogout(),
      error: () => this.finishLogout()
    });
  }

  hasRole(role: string): boolean {
    return this.user?.roles.includes(role) ?? false;
  }

  hasAnyRole(roles: string[]): boolean {
    return roles.some((role) => this.hasRole(role));
  }

  clearSession(): void {
    localStorage.removeItem("rk_auth");
    this.subject.next(null);
  }

  expireSession(): void {
    this.clearSession();
    void this.router.navigateByUrl("/login");
  }

  private storeUser(response: LoginResponse): void {
    localStorage.setItem("rk_auth", JSON.stringify(response));
    this.subject.next(response);
  }

  private finishLogout(): void {
    this.clearSession();
    void this.router.navigateByUrl("/login");
  }

  private readStoredUser(): LoginResponse | null {
    const raw = localStorage.getItem("rk_auth");

    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as LoginResponse;
    } catch {
      localStorage.removeItem("rk_auth");
      return null;
    }
  }
}
