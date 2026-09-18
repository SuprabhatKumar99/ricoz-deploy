import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { ApiService } from "../core/api.service";
import { SearchHit, SearchResult } from "../models";

@Component({
  selector: "rk-portal-search",
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="min-h-screen bg-white">
      <header class="border-b border-slate-200">
        <div class="mx-auto flex max-w-5xl items-center justify-between px-4 py-5">
          <a routerLink="/portal" [queryParams]="{ tenant: tenantSlug }" class="font-bold">RicozKnow</a>
          <a routerLink="/login" class="rk-btn rk-btn-secondary">Sign in</a>
        </div>
      </header>

      <main class="mx-auto max-w-5xl px-4 py-10">
        <form class="flex flex-col gap-2 sm:flex-row" (ngSubmit)="search()">
          <input class="rk-input" [(ngModel)]="query" name="query" aria-label="Search" />
          <button class="rk-btn rk-btn-primary">Search</button>
        </form>

        <div *ngIf="result" class="mt-8">
          <p class="text-sm text-slate-500">{{ result.total }} results for “{{ result.query }}”</p>

          <div class="mt-4 space-y-3">
            <a
              *ngFor="let item of result.results"
              [routerLink]="['/portal/article', item.slug]"
              class="block rounded-2xl border border-slate-200 p-5 hover:bg-slate-50"
            >
              <h2 class="font-bold">{{ item.title }}</h2>
              <p class="mt-2 text-sm leading-6 text-slate-500">{{ item.excerpt }}</p>
              <p class="mt-3 text-xs font-semibold text-slate-400">Score {{ item.score | number:"1.2-2" }}</p>
            </a>
          </div>

          <div *ngIf="!result.results.length" class="mt-6 rounded-2xl border border-slate-200 p-8 text-center">
            <p class="font-bold">No results</p>
            <p class="mt-2 text-sm text-slate-500">Try different words or browse the portal categories.</p>
          </div>
        </div>
      </main>
    </div>
  `
})
export class PortalSearchComponent implements OnInit {
  query = "";
  result?: SearchResult;

  constructor(
    private readonly api: ApiService,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.query = this.route.snapshot.queryParamMap.get("q") ?? "";

    if (this.query) {
      this.search();
    }
  }

  search(): void {
    if (!this.query.trim()) {
      return;
    }

    this.api.portalSearch(this.query.trim()).subscribe((result) => {
      this.result = result;
    });
  }

  get tenantSlug(): string | null {
    return this.route.snapshot.queryParamMap.get("tenant");
  }
}
