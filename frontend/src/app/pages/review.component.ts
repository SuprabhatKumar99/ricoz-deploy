import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { RouterLink } from "@angular/router";
import { ApiService } from "../core/api.service";
import { Article, ArticleVersion, Page } from "../models";
import { StatusComponent } from "../shared/ui";

type RichContentBlockType = "heading" | "paragraph" | "image" | "list" | "code";

interface RichContentBlock {
  type: RichContentBlockType;
  text?: string;
  level?: number;
  items?: string[];
  url?: string;
  alt?: string;
}

interface ReviewItem {
  article: Article;
  version: ArticleVersion;
  contentBlocks: RichContentBlock[];
}

@Component({
  selector: "rk-review",
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, StatusComponent],
  template: `
    <div>
      <div>
        <p class="text-sm font-semibold text-slate-500">Knowledge Studio</p>
        <h1 class="mt-1 text-3xl font-bold tracking-tight">Review queue</h1>
        <p class="mt-2 text-sm text-slate-500">
          Versions currently returned with IN_REVIEW status.
        </p>
      </div>

      <div class="mt-7 grid gap-6">
        <section *ngFor="let item of items" class="rk-card p-6">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <div class="flex items-center gap-3">
                <h2 class="text-xl font-bold">{{ item.article.title }}</h2>
                <rk-status [status]="item.version.status" />
              </div>
              <p class="mt-2 text-sm text-slate-500">
                Version {{ item.version.versionNumber }} · {{ item.version.changeSummary || "No change summary" }}
              </p>
            </div>

            <a [routerLink]="['/articles', item.article.id]" class="rk-btn rk-btn-secondary">
              Open article
            </a>
          </div>

          <div class="mt-5 grid gap-5 lg:grid-cols-[minmax(0,1.25fr)_minmax(320px,0.75fr)]">
            <div>
              <p class="text-xs font-bold uppercase tracking-widest text-slate-400">Content</p>

              <article class="mt-2 rounded-xl border border-slate-200 bg-white p-5 sm:p-6">
                <ng-container *ngIf="item.contentBlocks.length; else emptyContent">
                  <div class="space-y-6">
                    <ng-container *ngFor="let block of item.contentBlocks">
                      <h1
                        *ngIf="block.type === 'heading' && block.level === 1"
                        class="text-3xl font-bold tracking-tight text-slate-950"
                      >
                        {{ block.text }}
                      </h1>

                      <h2
                        *ngIf="block.type === 'heading' && block.level === 2"
                        class="text-2xl font-bold tracking-tight text-slate-950"
                      >
                        {{ block.text }}
                      </h2>

                      <h3
                        *ngIf="block.type === 'heading' && block.level === 3"
                        class="text-xl font-bold text-slate-950"
                      >
                        {{ block.text }}
                      </h3>

                      <h4
                        *ngIf="block.type === 'heading' && block.level === 4"
                        class="text-lg font-bold text-slate-950"
                      >
                        {{ block.text }}
                      </h4>

                      <p
                        *ngIf="block.type === 'paragraph'"
                        class="whitespace-pre-wrap text-base leading-8 text-slate-700"
                      >
                        {{ block.text }}
                      </p>

                      <pre
                        *ngIf="block.type === 'code'"
                        class="overflow-x-auto rounded-xl bg-slate-950 p-5 font-mono text-sm leading-6 text-white"
                      >{{ block.text }}</pre>

                      <ul
                        *ngIf="block.type === 'list'"
                        class="list-disc space-y-2 pl-6 text-base leading-7 text-slate-700"
                      >
                        <li *ngFor="let listItem of block.items">{{ listItem }}</li>
                      </ul>

                      <figure *ngIf="block.type === 'image' && block.url" class="space-y-2">
                        <img
                          [src]="block.url"
                          [alt]="block.alt || ''"
                          class="max-h-[520px] max-w-full rounded-xl border border-slate-200 object-contain"
                        />
                        <figcaption *ngIf="block.alt" class="text-sm text-slate-500">
                          {{ block.alt }}
                        </figcaption>
                      </figure>
                    </ng-container>
                  </div>
                </ng-container>

                <ng-template #emptyContent>
                  <p class="text-sm text-slate-500">No renderable content blocks were returned.</p>
                </ng-template>
              </article>
            </div>

            <div>
              <label class="rk-label" [for]="'comment-' + item.version.id">Review comment</label>
              <textarea
                class="rk-textarea min-h-32"
                [id]="'comment-' + item.version.id"
                [(ngModel)]="comments[item.version.id]"
                [name]="'comment-' + item.version.id"
                placeholder="Optional reviewer comment"
              ></textarea>

              <div class="mt-3 flex flex-wrap gap-2">
                <button class="rk-btn rk-btn-primary" (click)="decide(item, true)">
                  Approve
                </button>
                <button class="rk-btn rk-btn-danger" (click)="decide(item, false)">
                  Reject
                </button>
              </div>
            </div>
          </div>
        </section>

        <div *ngIf="!items.length" class="rk-card px-6 py-12 text-center">
          <h2 class="text-lg font-bold">No articles awaiting review</h2>
          <p class="mt-2 text-sm text-slate-500">
            The queue is empty for the articles accessible to this reviewer.
          </p>
        </div>
      </div>
    </div>
  `
})
export class ReviewComponent implements OnInit {
  items: ReviewItem[] = [];
  comments: Record<string, string> = {};

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.listArticles("IN_REVIEW", 0, 100).subscribe((page: Page<Article>) => {
      const requests = page.content.map((article) => {
        return new Promise<ReviewItem | null>((resolve) => {
          this.api.getVersions(article.id).subscribe((versions) => {
            const version = versions.find((item) => item.status === "IN_REVIEW");

            resolve(version ? { article, version, contentBlocks: this.richBlocks(version.content) } : null);
          });
        });
      });

      Promise.all(requests).then((items) => {
        this.items = items.filter((item): item is ReviewItem => item !== null);
      });
    });
  }

  decide(item: ReviewItem, approve: boolean): void {
    if (!confirm(approve ? "Approve this version?" : "Reject this version?")) {
      return;
    }

    this.api.reviewVersion(
      item.article.id,
      item.version.id,
      approve,
      this.comments[item.version.id] ?? ""
    ).subscribe(() => {
      this.load();
    });
  }

  richBlocks(content: unknown): RichContentBlock[] {
    if (!content || typeof content !== "object" || Array.isArray(content)) {
      return [];
    }

    const blocks = (content as { blocks?: unknown }).blocks;

    if (!Array.isArray(blocks)) {
      return [];
    }

    return blocks
      .filter((block): block is Record<string, unknown> => !!block && typeof block === "object" && !Array.isArray(block))
      .map((block) => {
        const type = block["type"];

        if (!this.isRichContentBlockType(type)) {
          return null;
        }

        const result: RichContentBlock = { type };
        const text = block["text"];
        const level = block["level"];
        const items = block["items"];
        const url = block["url"];
        const alt = block["alt"];

        if (typeof text === "string") {
          result.text = text;
        }
        if (typeof level === "number") {
          result.level = level;
        }
        if (Array.isArray(items)) {
          result.items = items.filter((item): item is string => typeof item === "string");
        }
        if (typeof url === "string") {
          result.url = url;
        }
        if (typeof alt === "string") {
          result.alt = alt;
        }

        return result;
      })
      .filter((block): block is RichContentBlock => block !== null);
  }

  private isRichContentBlockType(value: unknown): value is RichContentBlockType {
    return value === "heading"
      || value === "paragraph"
      || value === "image"
      || value === "list"
      || value === "code";
  }
}
