import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { ApiService } from "../core/api.service";
import { PortalArticleDetail } from "../models";

type RichTextBlock = {
  type: "heading" | "paragraph" | "image" | "list" | "code";
  text?: string;
  level?: number;
  items?: string[];
  assetId?: string;
};

@Component({
  selector: "rk-portal-article",
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="min-h-screen bg-white">
      <header class="border-b border-slate-200">
        <div class="mx-auto flex max-w-5xl items-center justify-between px-4 py-5">
          <a routerLink="/portal" [queryParams]="{ tenant: tenantSlug }" class="font-bold">RicozKnow</a>
          <a routerLink="/portal" [queryParams]="{ tenant: tenantSlug }" class="text-sm font-semibold text-slate-500">Search</a>
        </div>
      </header>

      <main class="mx-auto max-w-4xl px-4 py-10 sm:py-14">
        <a
          routerLink="/portal"
          class="text-sm font-semibold text-slate-500 hover:text-slate-950"
        >
          ← Knowledge base
        </a>

        <article *ngIf="article" class="mt-8">
          <p class="text-xs font-bold uppercase tracking-widest text-slate-400">
            Article
          </p>

          <h1 class="mt-3 text-4xl font-bold tracking-tight">
            {{ article.title }}
          </h1>

          <p class="mt-3 text-sm text-slate-500">
            Version {{ article.versionNumber }} · Published
          </p>

          <div class="my-10 border-t border-slate-200"></div>

          <div class="space-y-6">
            @for (block of contentBlocks; track $index) {
              @switch (block.type) {
                @case ("heading") {
                  @switch (block.level) {
                    @case (1) {
                      <h1 class="text-3xl font-bold text-slate-950">
                        {{ block.text }}
                      </h1>
                    }

                    @case (2) {
                      <h2 class="text-2xl font-bold text-slate-950">
                        {{ block.text }}
                      </h2>
                    }

                    @case (3) {
                      <h3 class="text-xl font-semibold text-slate-950">
                        {{ block.text }}
                      </h3>
                    }

                    @default {
                      <h4 class="text-lg font-semibold text-slate-950">
                        {{ block.text }}
                      </h4>
                    }
                  }
                }

                @case ("paragraph") {
                  <p class="text-base leading-8 text-slate-700">
                    {{ block.text }}
                  </p>
                }

                @case ("list") {
                  <ul
                    class="list-disc space-y-2 pl-6 text-base leading-7 text-slate-700"
                  >
                    @for (item of block.items ?? []; track $index) {
                      <li>{{ item }}</li>
                    }
                  </ul>
                }

                @case ("code") {
                  <pre
                    class="overflow-x-auto rounded-xl bg-slate-950 p-4 text-sm leading-6 text-slate-100"
                  ><code>{{ block.text }}</code></pre>
                }

                @case ("image") {
                  <div
                    class="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-500"
                  >
                    Image asset: {{ block.assetId }}
                  </div>
                }
              }
            }
          </div>

          <div class="my-10 border-t border-slate-200"></div>

          <section class="rounded-2xl border border-slate-200 p-6">
            <p class="font-bold">Was this article helpful?</p>

            <div class="mt-3 flex gap-2">
              <button
                class="rk-btn rk-btn-secondary"
                (click)="feedback(true)"
              >
                Yes
              </button>

              <button
                class="rk-btn rk-btn-secondary"
                (click)="feedback(false)"
              >
                No
              </button>
            </div>

            <p
              *ngIf="sent"
              class="mt-3 text-sm text-emerald-700"
            >
              Thanks for your feedback.
            </p>
          </section>
        </article>
      </main>
    </div>
  `
})
export class PortalArticleComponent implements OnInit {
  article?: PortalArticleDetail;
  sent = false;

  constructor(
    private readonly api: ApiService,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get("slug");

    if (slug) {
      this.api.portalArticle(slug).subscribe((article) => {
        this.article = article;
      });
    }
  }

  get contentBlocks(): RichTextBlock[] {
    if (!this.article?.content) {
      return [];
    }

    const content = this.article.content as {
      blocks?: unknown;
    };

    if (!Array.isArray(content.blocks)) {
      return [];
    }

    return content.blocks.filter(
      (block): block is RichTextBlock =>
        typeof block === "object" &&
        block !== null &&
        "type" in block &&
        typeof (block as { type?: unknown }).type === "string"
    );
  }

  feedback(helpful: boolean): void {
    if (!this.article) {
      return;
    }

    this.api.portalFeedback(this.article.id, helpful).subscribe(() => {
      this.sent = true;
    });
  }

  get tenantSlug(): string | null {
    return this.route.snapshot.queryParamMap.get("tenant");
  }
}