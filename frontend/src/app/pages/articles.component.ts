import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { RouterLink } from "@angular/router";
import { ApiService } from "../core/api.service";
import { Article, Category, Page, SearchHit } from "../models";
import { StatusComponent } from "../shared/ui";


@Component({
  selector: "rk-articles",
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, StatusComponent],
  template: `
    <div>
      <div class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p class="text-sm font-semibold text-slate-500">Knowledge Studio</p>
          <h1 class="mt-1 text-3xl font-bold tracking-tight">Articles</h1>
          <p class="mt-2 text-sm text-slate-500">
            Table-first browsing for governed article content.
          </p>
        </div>
        <a routerLink="/articles/new" class="rk-btn rk-btn-primary">+ New article</a>
      </div>

      <div class="rk-card mt-7 p-4">
        <div class="grid gap-3 md:grid-cols-[1fr_180px_auto]">
          <input
            class="rk-input"
            placeholder="Search articles..."
            [(ngModel)]="query"
            (keyup.enter)="search()"
            aria-label="Search articles"
          />

          <select class="rk-select" [(ngModel)]="status" (change)="load()">
            <option value="">All statuses</option>
            <option value="DRAFT">Draft</option>
            <option value="IN_REVIEW">In review</option>
            <option value="PUBLISHED">Published</option>
            <option value="ARCHIVED">Archived</option>
          </select>

          <button class="rk-btn rk-btn-secondary" (click)="load()">Refresh</button>
        </div>

        <div *ngIf="searchResults.length" class="mt-5 border-t border-slate-200 pt-5">
          <p class="text-sm font-semibold">{{ searchResults.length }} search results</p>
          <div class="mt-3 grid gap-2">
            <a
              *ngFor="let result of searchResults"
              [routerLink]="['/articles', result.articleId]"
              class="rounded-xl border border-slate-200 p-3 hover:bg-slate-50"
            >
              <p class="font-semibold">{{ result.title }}</p>
              <p class="mt-1 text-sm text-slate-500">{{ result.excerpt }}</p>
            </a>
          </div>
        </div>
      </div>

      <div class="rk-card mt-4 overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full min-w-[760px] text-left text-sm">
            <thead class="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
              <tr>
                <th class="px-5 py-3 font-semibold">Article</th>
                <th class="px-5 py-3 font-semibold">Status</th>
                <th class="px-5 py-3 font-semibold">Visibility</th>
                <th class="px-5 py-3 font-semibold">Category</th>
                <th class="px-5 py-3 font-semibold">Version</th>
                <th class="px-5 py-3"></th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr *ngFor="let article of page.content" class="hover:bg-slate-50/70">
                <td class="px-5 py-4">
                  <a [routerLink]="['/articles', article.id]" class="font-semibold hover:underline">
                    {{ article.title }}
                  </a>
                  <p class="mt-1 text-xs text-slate-500">{{ article.slug }}</p>
                </td>
                <td class="px-5 py-4">
                  <rk-status [status]="article.status" />
                </td>
                <td class="px-5 py-4 text-slate-600">{{ article.visibility }}</td>
                <td class="px-5 py-4 text-slate-600">{{ categoryName(article.categoryId) }}</td>
                <td class="px-5 py-4 text-slate-600">{{ article.activeVersionNumber ?? "—" }}</td>
                <td class="px-5 py-4 text-right">
                  <a [routerLink]="['/articles', article.id]" class="rk-btn rk-btn-secondary">Open</a>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div *ngIf="!page.content.length" class="border-t border-slate-200 px-5 py-12 text-center">
          <p class="font-semibold">No articles found</p>
          <p class="mt-1 text-sm text-slate-500">Try another status or create the first article.</p>
        </div>

        <div class="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 px-5 py-4 text-sm">
          <span class="text-slate-500">{{ page.totalElements }} total articles</span>
          <div class="flex gap-2">
            <button class="rk-btn rk-btn-secondary" [disabled]="page.first" (click)="load(page.number - 1)">
              Previous
            </button>
            <button class="rk-btn rk-btn-secondary" [disabled]="page.last" (click)="load(page.number + 1)">
              Next
            </button>
          </div>
        </div>
      </div>
    </div>
  `
})
export class ArticlesComponent implements OnInit {
  page: Page<Article> = {
    content: [],
    totalElements: 0,
    totalPages: 0,
    number: 0,
    size: 10,
    first: true,
    last: true
  };

  categories: Category[] = [];
  status = "";
  query = "";
  // searchResults = [];
  searchResults: SearchHit[] = [];

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.api.categories().subscribe((items) => {
      this.categories = items;
    });

    this.load();
  }

  load(page = 0): void {
    this.api.listArticles(this.status || undefined, page, 10).subscribe((result) => {
      this.page = result;
    });
  }

  search(): void {
    if (!this.query.trim()) {
      this.searchResults = [];
      return;
    }

    this.api.search(this.query.trim()).subscribe((result) => {
      this.searchResults = result.results;
    });
  }

  categoryName(id: string | null): string {
    return this.categories.find((category) => category.id === id)?.name ?? "—";
  }
}
