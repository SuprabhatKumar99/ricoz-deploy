import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ApiService } from "../core/api.service";
import { Tenant } from "../models";

@Component({
  selector: "rk-branding",
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div>
      <p class="text-sm font-semibold text-slate-500">Administration</p>

      <h1 class="mt-1 text-3xl font-bold tracking-tight">
        Portal branding
      </h1>

      <p class="mt-2 text-sm text-slate-500">
        These fields match the admin branding request exposed by the backend.
      </p>

      <div class="mt-7 grid gap-6 xl:grid-cols-[1fr_420px]">
        <form class="rk-card p-6" (ngSubmit)="save()">
          <h2 class="font-bold">Branding settings</h2>

          <div class="mt-5 space-y-5">
            <div>
              <label class="rk-label" for="portalTitle">
                Portal title
              </label>

              <input
                id="portalTitle"
                class="rk-input"
                [(ngModel)]="form.portalTitle"
                name="portalTitle"
              />
            </div>

            <div>
              <label class="rk-label" for="logoUrl">
                Logo URL
              </label>

              <input
                id="logoUrl"
                class="rk-input"
                [(ngModel)]="form.logoUrl"
                name="logoUrl"
              />
            </div>

            <div>
              <label class="rk-label" for="primaryColor">
                Primary color
              </label>

              <input
                id="primaryColor"
                class="rk-input"
                [(ngModel)]="form.primaryColor"
                name="primaryColor"
                placeholder="#2563eb"
              />
            </div>

            <p
              *ngIf="message"
              class="rounded-xl bg-emerald-50 p-3 text-sm text-emerald-700"
            >
              {{ message }}
            </p>

            <p
              *ngIf="error"
              class="rounded-xl bg-red-50 p-3 text-sm text-red-700"
            >
              {{ error }}
            </p>

            <button
              type="submit"
              class="rk-btn rk-btn-primary"
              [disabled]="saving"
            >
              {{ saving ? "Saving..." : "Save branding" }}
            </button>
          </div>
        </form>

        <section class="rk-card overflow-hidden">
          <div class="border-b border-slate-200 p-5">
            <h2 class="font-bold">Portal preview</h2>
          </div>

          <div class="p-5">
            <div
              class="overflow-hidden rounded-2xl border border-slate-200 bg-white"
            >
              <div
                class="flex items-center gap-3 border-b border-slate-200 p-4"
              >
                <img
                  *ngIf="form.logoUrl"
                  [src]="form.logoUrl"
                  [alt]="form.portalTitle || 'Portal logo'"
                  class="h-8 w-8 rounded-lg object-cover"
                />

                <div
                  *ngIf="!form.logoUrl"
                  class="grid h-8 w-8 place-items-center rounded-lg text-sm font-black text-white"
                  [style.background-color]="form.primaryColor || '#0f172a'"
                >
                  R
                </div>

                <span class="font-bold">
                  {{ form.portalTitle || "Knowledge Portal" }}
                </span>
              </div>

              <div class="p-8 text-center">
                <p
                  class="text-xs font-bold uppercase tracking-widest text-slate-400"
                >
                  Knowledge base
                </p>

                <p class="mt-3 text-2xl font-bold">
                  How can we help?
                </p>

                <div
                  class="mt-5 h-11 rounded-xl border border-slate-300"
                  [style.border-color]="form.primaryColor || '#cbd5e1'"
                ></div>

                <button
                  type="button"
                  class="mt-5 rounded-xl px-4 py-2 text-sm font-semibold text-white"
                  [style.background-color]="form.primaryColor || '#0f172a'"
                >
                  Search
                </button>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  `
})
export class BrandingComponent implements OnInit {
  form = {
    portalTitle: "",
    logoUrl: "",
    primaryColor: ""
  };

  message = "";
  error = "";
  saving = false;

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.loadBranding();
  }

  save(): void {
    this.message = "";
    this.error = "";
    this.saving = true;

    this.api.updateBranding(this.form).subscribe({
      next: (tenant) => {
        this.setTenant(tenant);
        this.message = "Branding saved.";
        this.saving = false;
      },
      error: (error) => {
        this.error =
          error?.error?.message ||
          "Unable to save branding.";
        this.saving = false;
      }
    });
  }

  private loadBranding(): void {
    this.message = "";
    this.error = "";

    /*
     * This is an authenticated admin page.
     *
     * The backend resolves the tenant from the authenticated
     * user's JWT, so we must not depend on the browser hostname
     * or subdomain here.
     */
    this.api.getCurrentBranding().subscribe({
      next: (tenant) => {
        this.setTenant(tenant);
      },
      error: (error) => {
        this.error =
          error?.error?.message ||
          "Unable to load branding.";
      }
    });
  }

  private setTenant(tenant: Tenant): void {
    this.form = {
      portalTitle: tenant.portalTitle || "",
      logoUrl: tenant.logoUrl || "",
      primaryColor: tenant.primaryColor || ""
    };
  }
}