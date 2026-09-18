import { CommonModule } from "@angular/common";
import { Component, inject } from "@angular/core";
import { RouterLink, RouterLinkActive, RouterOutlet } from "@angular/router";
import { AuthService } from "../core/auth.service";
import { ApiService } from "../core/api.service";

interface NavItem {
  label: string;
  path: string;
  roles?: string[];
}

@Component({
  selector: "rk-shell",
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="min-h-screen bg-slate-50">
      <div
        *ngIf="mobileOpen"
        class="fixed inset-0 z-30 bg-slate-950/30 lg:hidden"
        (click)="mobileOpen = false"
      ></div>

      <aside
        class="fixed inset-y-0 left-0 z-40 flex w-72 flex-col border-r border-slate-200 bg-white transition-transform lg:translate-x-0"
        [class.-translate-x-full]="!mobileOpen"
      >
        <div class="flex h-16 items-center gap-3 border-b border-slate-200 px-5">
          <div class="grid h-9 w-9 place-items-center rounded-xl bg-slate-900 font-black text-white">R</div>
          <div>
            <p class="font-bold tracking-tight">RicozKnow</p>
            <p class="text-[11px] text-slate-400">Knowledge Studio</p>
          </div>
        </div>

        <nav class="flex-1 overflow-y-auto px-3 py-5">
          <p class="px-3 text-[11px] font-bold uppercase tracking-widest text-slate-400">Workspace</p>

          <div class="mt-2 space-y-1">
            <a
              *ngFor="let item of workspaceNav"
              [class.hidden]="!canShow(item)"
              [routerLink]="item.path"
              routerLinkActive="bg-slate-100 text-slate-950"
              class="block rounded-xl px-3 py-2.5 text-sm font-medium text-slate-600 hover:bg-slate-50"
              (click)="mobileOpen = false"
            >
              {{ item.label }}
            </a>
          </div>

          <p class="mt-7 px-3 text-[11px] font-bold uppercase tracking-widest text-slate-400">Administration</p>

          <div class="mt-2 space-y-1">
            <a
              *ngFor="let item of adminNav"
              [class.hidden]="!canShow(item)"
              [routerLink]="item.path"
              routerLinkActive="bg-slate-100 text-slate-950"
              class="block rounded-xl px-3 py-2.5 text-sm font-medium text-slate-600 hover:bg-slate-50"
              (click)="mobileOpen = false"
            >
              {{ item.label }}
            </a>
          </div>
        </nav>

        <div class="border-t border-slate-200 p-4">
          <div class="flex items-center gap-3 rounded-xl bg-slate-50 p-3">
            <div class="grid h-9 w-9 place-items-center rounded-full bg-slate-200 text-sm font-bold">
              {{ initials }}
            </div>
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-semibold">{{ auth.user?.email }}</p>
              <p class="truncate text-xs text-slate-500">{{ auth.user?.roles?.join(", ") }}</p>
            </div>
          </div>
        </div>
      </aside>

      <header class="sticky top-0 z-20 border-b border-slate-200 bg-white/95 backdrop-blur lg:ml-72">
        <div class="flex h-16 items-center justify-between gap-4 px-4 sm:px-6">
          <button
            class="grid h-10 w-10 place-items-center rounded-xl border border-slate-200 lg:hidden"
            aria-label="Open navigation"
            (click)="mobileOpen = true"
          >
            ☰
          </button>

          <div class="hidden min-w-0 lg:block">
            <p class="text-xs font-medium text-slate-400">Knowledge Studio</p>
            <p class="truncate text-sm font-semibold">Governed knowledge for your organization</p>
          </div>

          <div class="ml-auto flex items-center gap-2">
            <a routerLink="/portal" [queryParams]="{ tenant: tenantSlug }" class="rk-btn rk-btn-secondary">Open portal</a>
            <button class="rk-btn rk-btn-secondary" (click)="auth.logout()">Sign out</button>
          </div>
        </div>
      </header>

      <main class="lg:ml-72">
        <div class="mx-auto max-w-[1500px] p-4 sm:p-6 lg:p-8">
          <router-outlet />
        </div>
      </main>
    </div>
  `
})
export class ShellComponent {
  // readonly auth = inject(AuthService);

  constructor(readonly auth: AuthService , private readonly api: ApiService){}

  mobileOpen = false;
  tenantSlug = "";

  readonly workspaceNav: NavItem[] = [
    { label: "Dashboard", path: "/dashboard" },
    { label: "Articles", path: "/articles" },
    { label: "Review queue", path: "/review", roles: ["ADMIN", "REVIEWER"] },
    { label: "Categories", path: "/categories" },
    { label: "Analytics", path: "/analytics", roles: ["ADMIN"] }
  ];

  readonly adminNav: NavItem[] = [
    { label: "Users", path: "/users", roles: ["ADMIN"] },
    { label: "Branding", path: "/branding", roles: ["ADMIN"] },
    { label: "Synonyms", path: "/synonyms", roles: ["ADMIN"] },
    { label: "Admin tools", path: "/admin-tools", roles: ["ADMIN"] },
    { label: "Agent access", path: "/agent", roles: ["ADMIN", "AGENT_VIEWER"] }
  ];

  ngOnInit(): void {
    this.api.getCurrentTenant().subscribe({
      next: (tenant) => {
        this.tenantSlug = tenant.slug;
      }, error: () => {
        this.tenantSlug = "";
      }
    });
  }

  canShow(item: NavItem): boolean {
    return !item.roles || this.auth.hasAnyRole(item.roles);
  }

  get initials(): string {
    const email = this.auth.user?.email ?? "";
    return email.slice(0, 2).toUpperCase();
  }

}
