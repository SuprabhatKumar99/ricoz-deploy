import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { RouterLink } from "@angular/router";
import { ApiService } from "../core/api.service";
import { Category, PortalArticleSummary, SearchResult, Tenant } from "../models";

@Component({
  selector: "rk-portal",
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="min-h-screen bg-white">
      <header class="border-b border-slate-200">
        <div class="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-5 sm:px-6">
          <div class="flex min-w-0 items-center gap-3">
            <img
              *ngIf="tenant?.logoUrl"
              [src]="tenant?.logoUrl"
              alt=""
              class="h-9 w-9 rounded-xl object-cover"
            />
            <div
              *ngIf="!tenant?.logoUrl"
              class="grid h-9 w-9 place-items-center rounded-xl font-black text-white"
              [style.background]="tenant?.primaryColor || '#0f172a'"
            >
              R
            </div>
            <div class="min-w-0">
              <p class="truncate font-bold">{{ tenant?.portalTitle || tenant?.name || "Knowledge Portal" }}</p>
              <p class="truncate text-xs text-slate-500">{{ tenant?.name }}</p>
            </div>
          </div>

          <a routerLink="/login" class="rk-btn rk-btn-secondary">Sign in</a>
        </div>
      </header>

      <main class="mx-auto max-w-6xl px-4 py-14 sm:px-6">
        <section class="mx-auto max-w-3xl text-center">
          <p class="text-xs font-bold uppercase tracking-[0.2em] text-slate-400">Knowledge base</p>
          <h1 class="mt-4 text-4xl font-bold tracking-tight sm:text-5xl">How can we help?</h1>
          <p class="mx-auto mt-4 max-w-xl text-sm leading-6 text-slate-500">
            Search the published knowledge for this tenant.
          </p>

          <form class="mt-7 flex flex-col gap-2 sm:flex-row" (ngSubmit)="search()">
            <input
              class="rk-input"
              [(ngModel)]="query"
              name="query"
              placeholder="Search for an answer…"
              aria-label="Search knowledge base"
            />
            <button class="rk-btn rk-btn-primary sm:px-6">Search</button>
          </form>
        </section>

        <section class="mt-12">
          <div class="flex items-center justify-between gap-4">
            <div>
              <h2 class="text-lg font-bold">Popular topics</h2>
              <p class="mt-1 text-sm text-slate-500">Browse the available categories.</p>
            </div>
          </div>

          <div class="mt-4 flex flex-wrap gap-2">
            <button
              *ngFor="let category of categories"
              class="rk-btn rk-btn-secondary"
              (click)="browse(category.id)"
            >
              {{ category.name }}
            </button>
          </div>
        </section>

        <section class="mt-12">
          <div class="flex items-center justify-between gap-4">
            <div>
              <h2 class="text-lg font-bold">{{ searchResult ? "Search results" : "Recommended articles" }}</h2>
              <p *ngIf="searchResult" class="mt-1 text-sm text-slate-500">
                {{ searchResult.total }} results for “{{ searchResult.query }}”
              </p>
            </div>
            <a *ngIf="searchResult" routerLink="/portal" class="text-sm font-semibold underline underline-offset-4">
              Clear
            </a>
          </div>

          <div class="mt-5 grid gap-4 md:grid-cols-2">
            <a
              *ngFor="let article of articles"
              [routerLink]="['/portal/article', article.slug]"
              [queryParams]="{ tenant: tenantSlug }"
              class="rounded-2xl border border-slate-200 p-6 transition hover:-translate-y-0.5 hover:shadow-sm"
            >
              <p class="text-xs font-bold uppercase tracking-widest text-slate-400">Article</p>
              <h3 class="mt-2 text-lg font-bold">{{ article.title }}</h3>
              <p class="mt-2 text-sm text-slate-500">{{ article.slug }}</p>
            </a>
          </div>

          <div *ngIf="searchResult && !articles.length" class="mt-5 rounded-2xl border border-slate-200 p-8 text-center">
            <p class="font-bold">No results for “{{ searchResult.query }}”</p>
            <p class="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-500">
              Try different words or browse one of the available categories.
            </p>
          </div>
        </section>
      </main>
    </div>
  `
})
export class PortalComponent implements OnInit {
  tenant?: Tenant;
  categories: Category[] = [];
  articles: PortalArticleSummary[] = [];
  searchResult?: SearchResult;
  query = "";

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.loadTenantFromHost();

    this.api.categories().subscribe((items) => {
      this.categories = items;
    });

    this.api.portalArticles(undefined, 0).subscribe((page) => {
      this.articles = page.content;
    });
  }

  browse(categoryId: string): void {
    this.searchResult = undefined;

    this.api.portalArticles(categoryId, 0).subscribe((page) => {
      this.articles = page.content;
    });
  }

  search(): void {
    if (!this.query.trim()) {
      return;
    }

    this.api.portalSearch(this.query.trim(), undefined, 0).subscribe((result) => {
      this.searchResult = result;
      this.articles = result.results.map((item) => ({
        id: item.articleId,
        slug: item.slug,
        title: item.title,
        categoryId: item.categoryId
      }));
    });
  }

  // private loadTenantFromHost(): void {
  //   const host = window.location.hostname;
  //   const parts = host.split(".");

  //   if (parts.length < 3 || host === "localhost") {
  //     return;
  //   }

  //   this.api.tenantLookup(parts[0]).subscribe((tenant) => {
  //     this.tenant = tenant;
  //   });
  // }

  private loadTenantFromHost(): void {
    const host = window.location.hostname;

    /*
    * Production:
    *   customer.ricozknow.com
    *   -> customer
    */
    if (host !== "localhost" && host !== "127.0.0.1") {
      const parts = host.split(".");

      if (parts.length >= 3) {
        this.api.tenantLookup(parts[0]).subscribe({
          next: (tenant) => {
            this.tenant = tenant;
          }
        });
      }

      return;
    }

    /*
    * Local development:
    *   http://localhost:4200/portal?tenant=testorg
    */
    const tenantSlug =
      new URLSearchParams(window.location.search).get("tenant");

    if (!tenantSlug) {
      return;
    }

    this.api.tenantLookup(tenantSlug).subscribe({
      next: (tenant) => {
        this.tenant = tenant;
      }
    });
  }

  get tenantSlug(): string | null {
    return new URLSearchParams(window.location.search).get("tenant");
  }
}
