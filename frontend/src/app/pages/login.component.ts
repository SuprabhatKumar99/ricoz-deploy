import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router, RouterLink } from "@angular/router";
import { AuthService } from "../core/auth.service";

@Component({
  selector: "rk-login",
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="grid min-h-screen lg:grid-cols-[1.05fr_0.95fr]">
      <section class="hidden bg-slate-950 p-12 text-white lg:flex lg:flex-col lg:justify-between">
        <div class="flex items-center gap-3">
          <div class="grid h-10 w-10 place-items-center rounded-xl bg-white font-black text-slate-950">R</div>
          <span class="text-xl font-bold">RicozKnow</span>
        </div>

        <div class="max-w-xl">
          <p class="text-sm font-semibold uppercase tracking-widest text-slate-500">Knowledge platform</p>
          <h1 class="mt-5 text-5xl font-bold leading-tight tracking-tight">
            Put trusted answers closer to every question.
          </h1>
          <p class="mt-6 text-lg leading-8 text-slate-400">
            Search, author, review and govern your organization's published knowledge.
          </p>
        </div>

        <p class="text-sm text-slate-500">Search first. Content second. Administration third.</p>
      </section>

      <section class="flex items-center justify-center bg-white p-6 sm:p-10">
        <div class="w-full max-w-md">
          <div class="lg:hidden">
            <div class="grid h-10 w-10 place-items-center rounded-xl bg-slate-900 font-black text-white">R</div>
            <p class="mt-4 text-xl font-bold">RicozKnow</p>
          </div>

          <div class="mt-10 lg:mt-0">
            <p class="text-sm font-semibold text-slate-500">Welcome back</p>
            <h2 class="mt-2 text-3xl font-bold tracking-tight">Sign in to your workspace</h2>
            <p class="mt-2 text-sm leading-6 text-slate-500">
              The backend requires a tenant slug, email and password.
            </p>
          </div>

          <form class="mt-8 space-y-5" (ngSubmit)="submit()">
            <div>
              <label class="rk-label" for="tenantSlug">Tenant slug</label>
              <input id="tenantSlug" class="rk-input" [(ngModel)]="form.tenantSlug" name="tenantSlug" required />
            </div>

            <div>
              <label class="rk-label" for="email">Email</label>
              <input id="email" class="rk-input" type="email" [(ngModel)]="form.email" name="email" required />
            </div>

            <div>
              <label class="rk-label" for="password">Password</label>
              <input id="password" class="rk-input" type="password" [(ngModel)]="form.password" name="password" required />
            </div>

            <div
              *ngIf="error"
              role="alert"
              class="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm leading-5 text-rose-700"
            >
              {{ error }}
            </div>

            <button class="rk-btn rk-btn-primary w-full" [disabled]="busy">
              {{ busy ? "Signing in…" : "Sign in" }}
            </button>
          </form>

          <p class="mt-7 text-sm text-slate-500">
            New organization?
            <a routerLink="/register" class="font-semibold text-slate-950 underline underline-offset-4">
              Register
            </a>
          </p>
        </div>
      </section>
    </div>
  `
})
export class LoginComponent {
  form = {
    tenantSlug: "",
    email: "",
    password: ""
  };

  busy = false;
  error = "";

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router
  ) {}

  submit(): void {
    this.busy = true;
    this.error = "";

    this.auth.login(this.form).subscribe({
      next: () => void this.router.navigateByUrl("/dashboard"),
      error: (error) => {
        this.error = error?.error?.message || "Sign in failed. Check your tenant, email and password.";
        this.busy = false;
      },
      complete: () => {
        this.busy = false;
      }
    });
  }
}
