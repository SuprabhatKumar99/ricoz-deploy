import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ApiService } from "../core/api.service";
import { ArticlePerformance, DeflectionSummary, KnowledgeGap, StaleContent } from "../models";
import { MetricCardComponent } from "../shared/ui";

@Component({
  selector: "rk-analytics",
  standalone: true,
  imports: [CommonModule, FormsModule, MetricCardComponent],
  template: `
    <div>
      <div class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p class="text-sm font-semibold text-slate-500">Administration</p>
          <h1 class="mt-1 text-3xl font-bold tracking-tight">Knowledge performance</h1>
          <p class="mt-2 text-sm text-slate-500">
            The backend exposes counts rather than a time-series chart endpoint, so this screen stays faithful to those response shapes.
          </p>
        </div>

        <div class="flex gap-2">
          <input class="rk-input w-24" type="number" min="1" [(ngModel)]="days" aria-label="Analytics window in days" />
          <button class="rk-btn rk-btn-primary" (click)="load()">Refresh</button>
        </div>
      </div>

      <div class="mt-7 grid gap-4 md:grid-cols-3">
        <rk-metric-card label="Estimated deflection" [value]="deflection?.estimatedDeflectionCount ?? '—'" />
        <rk-metric-card label="Confirmed deflection" [value]="deflection?.confirmedDeflectionCount ?? '—'" />
        <rk-metric-card label="Support contacts" [value]="deflection?.supportContactCount ?? '—'" />
      </div>

      <div class="mt-7 grid gap-6 xl:grid-cols-2">
        <section class="rk-card overflow-hidden">
          <div class="border-b border-slate-200 px-5 py-4">
            <h2 class="font-bold">Article performance</h2>
            <p class="mt-1 text-xs text-slate-500">Views and feedback counts for the selected window.</p>
          </div>

          <div class="overflow-x-auto">
            <table class="w-full min-w-[600px] text-left text-sm">
              <thead class="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th class="px-5 py-3">Article</th>
                  <th class="px-5 py-3">Views</th>
                  <th class="px-5 py-3">Helpful</th>
                  <th class="px-5 py-3">Unhelpful</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                <tr *ngFor="let item of performance">
                  <td class="px-5 py-4 font-semibold">{{ item.title }}</td>
                  <td class="px-5 py-4">{{ item.views }}</td>
                  <td class="px-5 py-4">{{ item.helpfulCount }}</td>
                  <td class="px-5 py-4">{{ item.unhelpfulCount }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="rk-card">
          <div class="border-b border-slate-200 px-5 py-4">
            <h2 class="font-bold">Knowledge gaps</h2>
            <p class="mt-1 text-xs text-slate-500">Returned by the knowledge-gaps endpoint.</p>
          </div>

          <div class="divide-y divide-slate-100">
            <div *ngFor="let item of gaps" class="px-5 py-4">
              <div class="flex items-center justify-between gap-4">
                <p class="font-semibold">{{ item.query }}</p>
                <span class="rk-badge bg-amber-100 text-amber-700">{{ item.gapScore }}</span>
              </div>
              <p class="mt-1 text-xs text-slate-500">
                {{ item.volumeCount }} searches · {{ item.zeroResultCount }} zero results · {{ item.unsuccessfulCount }} unsuccessful
              </p>
            </div>
          </div>
        </section>
      </div>

      <section class="rk-card mt-6 overflow-hidden">
        <div class="border-b border-slate-200 px-5 py-4">
          <h2 class="font-bold">Stale content</h2>
          <p class="mt-1 text-xs text-slate-500">Content within the requested expiry horizon.</p>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full min-w-[620px] text-left text-sm">
            <thead class="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
              <tr>
                <th class="px-5 py-3">Article</th>
                <th class="px-5 py-3">Expires</th>
                <th class="px-5 py-3">State</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr *ngFor="let item of stale">
                <td class="px-5 py-4 font-semibold">{{ item.title }}</td>
                <td class="px-5 py-4">{{ item.expiresAt | date:"medium" }}</td>
                <td class="px-5 py-4">{{ item.alreadyExpired ? "Expired" : "Upcoming" }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  `
})
export class AnalyticsComponent implements OnInit {
  days = 30;
  performance: ArticlePerformance[] = [];
  gaps: KnowledgeGap[] = [];
  stale: StaleContent[] = [];
  deflection?: DeflectionSummary;

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.performance(this.days).subscribe((items) => {
      this.performance = items;
    });

    this.api.knowledgeGaps(this.days, 20).subscribe((items) => {
      this.gaps = items;
    });

    this.api.deflection(this.days).subscribe((summary) => {
      this.deflection = summary;
    });

    this.api.staleContent(this.days).subscribe((items) => {
      this.stale = items;
    });
  }
}
