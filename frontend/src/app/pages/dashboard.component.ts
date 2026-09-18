import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { RouterLink } from "@angular/router";
import { ApiService } from "../core/api.service";
import { Article, DeflectionSummary, KnowledgeGap, StaleContent } from "../models";
import { MetricCardComponent, StatusComponent } from "../shared/ui";
import { AuthService } from "../core/auth.service";

@Component({
  selector: "rk-dashboard",
  standalone: true,
  imports: [CommonModule, RouterLink, MetricCardComponent, StatusComponent],
  template: `
    <div>
      <div class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p class="text-sm font-semibold text-slate-500">Overview</p>
          <h1 class="mt-1 text-3xl font-bold tracking-tight">What needs attention?</h1>
          <p class="mt-2 text-sm text-slate-500">
            Action-oriented signals from the backend's article and analytics endpoints.
          </p>
        </div>
        <a routerLink="/articles/new" class="rk-btn rk-btn-primary">+ New article</a>
      </div>

      <div class="mt-7 grid gap-4 md:grid-cols-3">
        <rk-metric-card label="Articles" [value]="articleCount" />
        <rk-metric-card label="Knowledge gaps" [value]="gaps.length" hint="top returned gaps" />
        <rk-metric-card label="Confirmed deflection" [value]="deflection?.confirmedDeflectionCount ?? '—'" />
      </div>

      <div class="mt-7 grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
        <section class="rk-card overflow-hidden">
          <div class="flex items-center justify-between border-b border-slate-200 px-5 py-4">
            <div>
              <h2 class="font-bold">Recent articles</h2>
              <p class="mt-1 text-xs text-slate-500">Current article list returned by the API.</p>
            </div>
            <a routerLink="/articles" class="text-sm font-semibold text-slate-700">View all</a>
          </div>

          <div class="divide-y divide-slate-100">
            <div *ngFor="let article of recentArticles" class="flex items-center gap-4 px-5 py-4">
              <div class="min-w-0 flex-1">
                <a [routerLink]="['/articles', article.id]" class="truncate font-semibold hover:underline">
                  {{ article.title }}
                </a>
                <p class="mt-1 truncate text-xs text-slate-500">{{ article.slug }}</p>
              </div>
              <rk-status [status]="article.status" />
            </div>

            <div *ngIf="!recentArticles.length" class="px-5 py-10 text-center text-sm text-slate-500">
              No articles returned.
            </div>
          </div>
        </section>

        <section class="rk-card">
          <div class="border-b border-slate-200 px-5 py-4">
            <h2 class="font-bold">Knowledge gaps</h2>
            <p class="mt-1 text-xs text-slate-500">High-volume queries returned by analytics.</p>
          </div>

          <div class="divide-y divide-slate-100">
            <div *ngFor="let gap of gaps.slice(0, 5)" class="px-5 py-4">
              <div class="flex items-center justify-between gap-4">
                <p class="font-medium">{{ gap.query }}</p>
                <span class="rk-badge bg-amber-100 text-amber-700">{{ gap.gapScore }}</span>
              </div>
              <p class="mt-1 text-xs text-slate-500">
                {{ gap.volumeCount }} searches · {{ gap.zeroResultCount }} zero results
              </p>
            </div>

            <div *ngIf="!gaps.length" class="px-5 py-10 text-center text-sm text-slate-500">
              No knowledge gaps returned for the current window.
            </div>
          </div>
        </section>
      </div>

      <section class="rk-card mt-6">
        <div class="border-b border-slate-200 px-5 py-4">
          <h2 class="font-bold">Freshness</h2>
          <p class="mt-1 text-xs text-slate-500">Content returned by the stale-content endpoint.</p>
        </div>

        <div class="divide-y divide-slate-100">
          <div *ngFor="let item of stale.slice(0, 5)" class="flex flex-wrap items-center justify-between gap-3 px-5 py-4">
            <div>
              <p class="font-semibold">{{ item.title }}</p>
              <p class="mt-1 text-xs text-slate-500">Expires {{ item.expiresAt | date:"mediumDate" }}</p>
            </div>
            <span
              class="rk-badge"
              [class.bg-rose-100]="item.alreadyExpired"
              [class.text-rose-700]="item.alreadyExpired"
              [class.bg-amber-100]="!item.alreadyExpired"
              [class.text-amber-700]="!item.alreadyExpired"
            >
              {{ item.alreadyExpired ? "Expired" : "Expiring" }}
            </span>
          </div>
        </div>
      </section>
    </div>
  `
})
export class DashboardComponent implements OnInit {
  recentArticles: Article[] = [];
  articleCount = 0;
  gaps: KnowledgeGap[] = [];
  stale: StaleContent[] = [];
  deflection?: DeflectionSummary;

  constructor(private readonly api: ApiService, readonly auth: AuthService) {}

  ngOnInit(): void {
    this.loadArticles();

    if (this.auth.hasRole("ADMIN")) {
      this.loadAnalytics();
    }
  }

  private loadArticles(): void {
    this.api.listArticles(undefined, 0, 5).subscribe({
      next: (page) => {
        this.recentArticles = page.content;
        this.articleCount = page.totalElements;
      }
    });
  }

  private loadAnalytics(): void {
    this.api.knowledgeGaps().subscribe({
      next: (items) => {
        this.gaps = items;
      }
    });

    this.api.staleContent().subscribe({
      next: (items) => {
        this.stale = items;
      }
    });

    this.api.deflection().subscribe({
      next: (summary) => {
        this.deflection = summary;
      }
    });
  }

}
