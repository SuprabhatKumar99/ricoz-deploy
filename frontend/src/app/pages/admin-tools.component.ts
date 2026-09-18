import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ApiService } from "../core/api.service";

@Component({
  selector: "rk-admin-tools",
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div>
      <p class="text-sm font-semibold text-slate-500">Administration</p>
      <h1 class="mt-1 text-3xl font-bold tracking-tight">Admin tools</h1>
      <p class="mt-2 text-sm text-slate-500">Operator actions that exist as explicit backend endpoints.</p>

      <div class="mt-7 grid gap-6 xl:grid-cols-2">
        <section class="rk-card p-6">
          <h2 class="text-lg font-bold">Search index</h2>
          <p class="mt-2 text-sm leading-6 text-slate-500">
            Rebuild the current tenant's OpenSearch index.
          </p>

          <button class="rk-btn rk-btn-primary mt-5" (click)="reindex()" [disabled]="busy">
            {{ busy ? "Rebuilding…" : "Reindex" }}
          </button>

          <p *ngIf="reindexMessage" class="mt-4 text-sm text-emerald-700">{{ reindexMessage }}</p>
        </section>

        <section class="rk-card p-6">
          <h2 class="text-lg font-bold">Analytics aggregation</h2>
          <p class="mt-2 text-sm leading-6 text-slate-500">
            Trigger aggregation for an optional date. The backend defaults to yesterday when no date is provided.
          </p>

          <div class="mt-5 flex flex-wrap gap-2">
            <input class="rk-input max-w-xs" type="date" [(ngModel)]="date" aria-label="Aggregation date" />
            <button class="rk-btn rk-btn-primary" (click)="aggregate()">Run aggregation</button>
          </div>

          <p *ngIf="aggregationMessage" class="mt-4 text-sm text-emerald-700">{{ aggregationMessage }}</p>
        </section>
      </div>
    </div>
  `
})
export class AdminToolsComponent {
  date = "";
  busy = false;
  reindexMessage = "";
  aggregationMessage = "";

  constructor(private readonly api: ApiService) {}

  reindex(): void {
    this.busy = true;
    this.reindexMessage = "";

    this.api.reindex().subscribe({
      next: (result) => {
        this.reindexMessage = `${result.indexed} documents indexed.`;
        this.busy = false;
      },
      error: (error) => {
        this.reindexMessage = error?.error?.message || "Reindex failed.";
        this.busy = false;
      }
    });
  }

  aggregate(): void {
    this.api.aggregate(this.date || undefined).subscribe({
      next: (result) => {
        this.aggregationMessage = `${result.status} · ${result.date}`;
      },
      error: (error) => {
        this.aggregationMessage = error?.error?.message || "Aggregation failed.";
      }
    });
  }
}
