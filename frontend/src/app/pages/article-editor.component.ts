import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute, Router, RouterLink } from "@angular/router";
import { ApiService } from "../core/api.service";
import { Article, ArticleVersion, Category, Visibility } from "../models";
import { StatusComponent } from "../shared/ui";

type ContentBlockType = "heading" | "paragraph" | "image" | "list" | "code";

interface EditorBlock {
  type: ContentBlockType;
  text?: string;
  level?: number;
  items?: string[];
  url?: string;
  alt?: string;
}

@Component({
  selector: "rk-article-editor",
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, StatusComponent],
  template: `
    <div>
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div>
          <a routerLink="/articles" class="text-sm font-semibold text-slate-500 hover:text-slate-950">← Articles</a>
          <div class="mt-3 flex flex-wrap items-center gap-3">
            <h1 class="text-3xl font-bold tracking-tight">{{ article?.title || "New article" }}</h1>
            <rk-status *ngIf="version" [status]="version.status" />
          </div>
          <p *ngIf="article" class="mt-2 text-sm text-slate-500">
            v{{ version?.versionNumber ?? article.activeVersionNumber ?? "—" }} · {{ article.visibility }}
          </p>
        </div>

        <div class="flex flex-wrap gap-2">
          <button *ngIf="article && version" class="rk-btn rk-btn-secondary" (click)="preview = !preview">
            {{ preview ? "Edit" : "Preview" }}
          </button>
          <button *ngIf="article && canEdit" class="rk-btn rk-btn-secondary" (click)="newVersion()">
            New version
          </button>
          <button *ngIf="article && canEdit && version" class="rk-btn rk-btn-secondary" (click)="submit()">
            Submit for review
          </button>
          <button *ngIf="canEdit" class="rk-btn rk-btn-primary" (click)="save()">
            {{ saving ? "Saving…" : "Save draft" }}
          </button>
        </div>
      </div>

      <div *ngIf="message" role="status" class="mt-5 rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-700">
        {{ message }}
      </div>

      <div *ngIf="error" role="alert" class="mt-5 rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">
        {{ error }}
      </div>

      <div class="mt-6 grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
        <section class="rk-card overflow-hidden">
          <div *ngIf="preview; else editor" class="p-6 sm:p-8">
            <div class="mx-auto max-w-3xl">
              <p class="text-xs font-bold uppercase tracking-widest text-slate-400">Preview</p>
              <h2 class="mt-3 text-3xl font-bold">{{ article?.title || form.title }}</h2>

              <div class="mt-8 space-y-6">
                <ng-container *ngFor="let block of blocks">
                  <h3
                    *ngIf="block.type === 'heading'"
                    class="font-bold text-slate-950"
                    [class.text-2xl]="block.level === 1"
                    [class.text-xl]="block.level === 2"
                    [class.text-lg]="block.level === 3 || block.level === 4"
                  >
                    {{ block.text }}
                  </h3>

                  <p *ngIf="block.type === 'paragraph'" class="whitespace-pre-wrap text-base leading-8 text-slate-700">
                    {{ block.text }}
                  </p>

                  <pre *ngIf="block.type === 'code'" class="overflow-auto rounded-xl bg-slate-950 p-5 font-mono text-sm leading-6 text-white">{{ block.text }}</pre>

                  <ul *ngIf="block.type === 'list'" class="list-disc space-y-2 pl-6 text-base leading-7 text-slate-700">
                    <li *ngFor="let item of block.items">{{ item }}</li>
                  </ul>

                  <figure *ngIf="block.type === 'image' && block.url" class="space-y-2">
                    <img [src]="block.url" [alt]="block.alt || ''" class="max-h-[520px] rounded-xl border border-slate-200 object-contain" />
                    <figcaption *ngIf="block.alt" class="text-sm text-slate-500">{{ block.alt }}</figcaption>
                  </figure>
                </ng-container>
              </div>
            </div>
          </div>

          <ng-template #editor>
            <div class="border-b border-slate-200 px-6 py-4">
              <p class="text-sm font-bold">Article content</p>
              <p class="mt-1 text-xs text-slate-500">
                Use the structured editor below. It produces the backend's required <code>blocks</code> JSON object instead of storing the editor content as a JSON string.
              </p>
            </div>

            <div class="border-b border-slate-200 bg-slate-50 px-4 py-3 sm:px-6">
              <div class="flex flex-wrap gap-2">
                <button class="rk-btn rk-btn-secondary" [disabled]="!canEdit" (click)="addBlock('paragraph')">Paragraph</button>
                <button class="rk-btn rk-btn-secondary" [disabled]="!canEdit" (click)="addBlock('heading')">Heading</button>
                <button class="rk-btn rk-btn-secondary" [disabled]="!canEdit" (click)="addBlock('list')">List</button>
                <button class="rk-btn rk-btn-secondary" [disabled]="!canEdit" (click)="addBlock('code')">Code</button>
                <button class="rk-btn rk-btn-secondary" [disabled]="!canEdit" (click)="addBlock('image')">Image</button>
              </div>
            </div>

            <div class="p-4 sm:p-6">
              <div *ngIf="blocks.length === 0" class="rounded-xl border border-dashed border-slate-300 p-8 text-center">
                <p class="font-semibold text-slate-800">Start writing your article</p>
                <p class="mt-1 text-sm text-slate-500">Add a paragraph or heading from the toolbar above.</p>
              </div>

              <div class="space-y-4">
                <article *ngFor="let block of blocks; let index = index" class="rounded-xl border border-slate-200 bg-white p-4">
                  <div class="flex flex-wrap items-center justify-between gap-3">
                    <div class="flex items-center gap-2">
                      <span class="text-xs font-bold uppercase tracking-widest text-slate-400">{{ block.type }}</span>
                      <select
                        class="rk-select !mt-0 w-auto py-1.5 text-sm"
                        [(ngModel)]="block.type"
                        [name]="'block-type-' + index"
                        [disabled]="!canEdit"
                        (ngModelChange)="normalizeBlock(block)"
                      >
                        <option value="paragraph">Paragraph</option>
                        <option value="heading">Heading</option>
                        <option value="list">List</option>
                        <option value="code">Code</option>
                        <option value="image">Image</option>
                      </select>
                    </div>

                    <div class="flex gap-1">
                      <button class="rounded-lg px-2 py-1 text-sm text-slate-500 hover:bg-slate-100" [disabled]="!canEdit || index === 0" (click)="moveBlock(index, -1)" aria-label="Move block up">↑</button>
                      <button class="rounded-lg px-2 py-1 text-sm text-slate-500 hover:bg-slate-100" [disabled]="!canEdit || index === blocks.length - 1" (click)="moveBlock(index, 1)" aria-label="Move block down">↓</button>
                      <button class="rounded-lg px-2 py-1 text-sm text-rose-600 hover:bg-rose-50" [disabled]="!canEdit" (click)="removeBlock(index)">Remove</button>
                    </div>
                  </div>

                  <div *ngIf="block.type === 'heading'" class="mt-4 grid gap-4 sm:grid-cols-[120px_minmax(0,1fr)]">
                    <div>
                      <label class="rk-label" [for]="'heading-level-' + index">Level</label>
                      <select id="heading-level-{{ index }}" class="rk-select" [(ngModel)]="block.level" [name]="'heading-level-' + index" [disabled]="!canEdit">
                        <option [ngValue]="1">H1</option>
                        <option [ngValue]="2">H2</option>
                        <option [ngValue]="3">H3</option>
                        <option [ngValue]="4">H4</option>
                      </select>
                    </div>
                    <div>
                      <label class="rk-label" [for]="'heading-text-' + index">Heading</label>
                      <input id="heading-text-{{ index }}" class="rk-input text-lg font-semibold" [(ngModel)]="block.text" [name]="'heading-text-' + index" [disabled]="!canEdit" placeholder="Heading text" />
                    </div>
                  </div>

                  <div *ngIf="block.type === 'paragraph'" class="mt-4">
                    <label class="rk-label" [for]="'paragraph-text-' + index">Text</label>
                    <textarea id="paragraph-text-{{ index }}" class="rk-textarea min-h-[150px] resize-y text-base leading-7" [(ngModel)]="block.text" [name]="'paragraph-text-' + index" [disabled]="!canEdit" placeholder="Write your article paragraph..."></textarea>
                  </div>

                  <div *ngIf="block.type === 'code'" class="mt-4">
                    <label class="rk-label" [for]="'code-text-' + index">Code</label>
                    <textarea id="code-text-{{ index }}" class="rk-textarea min-h-[180px] resize-y font-mono text-sm leading-6" [(ngModel)]="block.text" [name]="'code-text-' + index" [disabled]="!canEdit" spellcheck="false" placeholder="Paste code here..."></textarea>
                  </div>

                  <div *ngIf="block.type === 'list'" class="mt-4">
                    <label class="rk-label">List items</label>
                    <div class="space-y-2">
                      <div *ngFor="let item of block.items; let itemIndex = index" class="flex gap-2">
                        <input class="rk-input !mt-0" [(ngModel)]="block.items![itemIndex]" [name]="'list-' + index + '-' + itemIndex" [disabled]="!canEdit" placeholder="List item" />
                        <button class="rounded-lg px-3 text-sm text-rose-600 hover:bg-rose-50" [disabled]="!canEdit" (click)="removeListItem(block, itemIndex)">Remove</button>
                      </div>
                    </div>
                    <button class="rk-btn rk-btn-secondary mt-3" [disabled]="!canEdit" (click)="addListItem(block)">Add item</button>
                  </div>

                  <div *ngIf="block.type === 'image'" class="mt-4 grid gap-4">
                    <div>
                      <label class="rk-label" [for]="'image-url-' + index">Image URL</label>
                      <input id="image-url-{{ index }}" class="rk-input" [(ngModel)]="block.url" [name]="'image-url-' + index" [disabled]="!canEdit" placeholder="https://..." />
                    </div>
                    <div>
                      <label class="rk-label" [for]="'image-alt-' + index">Alt text</label>
                      <input id="image-alt-{{ index }}" class="rk-input" [(ngModel)]="block.alt" [name]="'image-alt-' + index" [disabled]="!canEdit" placeholder="Describe the image" />
                    </div>
                    <img *ngIf="block.url" [src]="block.url" [alt]="block.alt || ''" class="max-h-72 rounded-xl border border-slate-200 object-contain" />
                  </div>
                </article>
              </div>

              <div class="mt-6 rounded-xl bg-slate-50 p-4">
                <div class="flex items-center justify-between gap-3">
                  <p class="text-xs font-bold uppercase tracking-widest text-slate-400">Generated content payload</p>
                  <button class="text-xs font-semibold text-slate-500 hover:text-slate-950" [disabled]="!canEdit" (click)="showJson = !showJson">
                    {{ showJson ? "Hide JSON" : "Show JSON" }}
                  </button>
                </div>
                <pre *ngIf="showJson" class="mt-3 overflow-auto text-xs leading-5 text-slate-600">{{ contentJson }}</pre>
              </div>
            </div>
          </ng-template>
        </section>

        <aside class="space-y-6">
          <section class="rk-card p-5">
            <h2 class="font-bold">Article settings</h2>

            <div class="mt-5">
              <label class="rk-label" for="title">Title</label>
              <input id="title" class="rk-input" [(ngModel)]="form.title" name="title" [disabled]="!!article || !canEdit" />
            </div>

            <div class="mt-5">
              <label class="rk-label" for="slug">Slug</label>
              <input id="slug" class="rk-input" [(ngModel)]="form.slug" name="slug" [disabled]="!!article || !canEdit" />
            </div>

            <div class="mt-5">
              <label class="rk-label" for="category">Category</label>
              <select
                id="category"
                class="rk-select"
                [(ngModel)]="form.categoryId"
                name="category"
                [disabled]="!!article || !canEdit"
              >
                <option value="">No category</option>
                <option *ngFor="let category of categories" [value]="category.id">
                  {{ category.name }}
                </option>
              </select>
            </div>

            <div *ngIf="article" class="mt-5">
              <label class="rk-label" for="visibility">Visibility</label>
              <select
                id="visibility"
                class="rk-select"
                [(ngModel)]="visibility"
                name="visibility"
                [disabled]="!canEdit"
              >
                <option value="PUBLIC">Public</option>
                <option value="AUTHENTICATED">Authenticated</option>
                <option value="PRIVATE">Private</option>
              </select>
              <button class="rk-btn rk-btn-secondary mt-3 w-full" [disabled]="!canEdit" (click)="setVisibility()">
                Update visibility
              </button>
            </div>

            <button
              *ngIf="article && canEdit"
              class="rk-btn rk-btn-danger mt-3 w-full"
              (click)="archive()"
            >
              Archive article
            </button>
          </section>

          <section class="rk-card p-5">
            <h2 class="font-bold">Version history</h2>

            <div class="mt-4 divide-y divide-slate-100">
              <button
                *ngFor="let item of versions"
                class="block w-full py-3 text-left"
                [class.bg-slate-50]="item.id === version?.id"
                (click)="selectVersion(item)"
              >
                <div class="flex items-center justify-between gap-3">
                  <span class="font-semibold">v{{ item.versionNumber }}</span>
                  <rk-status [status]="item.status" />
                </div>
                <p class="mt-1 text-xs text-slate-500">
                  {{ item.changeSummary || "No change summary" }}
                </p>
              </button>
            </div>
          </section>

          <section *ngIf="article && canEdit" class="rk-card p-5">
            <h2 class="font-bold">Assets</h2>
            <p class="mt-1 text-xs leading-5 text-slate-500">
              Upload uses the backend multipart asset endpoint.
            </p>

            <input
              class="mt-4 block w-full text-sm"
              type="file"
              (change)="uploadAsset($event)"
            />

            <a
              *ngIf="assetUrl"
              [href]="assetUrl"
              target="_blank"
              rel="noopener"
              class="mt-3 block break-all text-sm font-semibold text-slate-700 underline"
            >
              Open uploaded asset
            </a>
          </section>
        </aside>
      </div>
    </div>
  `
})
export class ArticleEditorComponent implements OnInit {
  article?: Article;
  versions: ArticleVersion[] = [];
  version?: ArticleVersion;
  categories: Category[] = [];
  blocks: EditorBlock[] = [];

  form = {
    title: "",
    slug: "",
    categoryId: ""
  };

  changeSummary = "";
  visibility: Visibility = "PUBLIC";
  preview = false;
  showJson = false;
  saving = false;
  message = "";
  error = "";
  assetUrl = "";

  constructor(
    private readonly api: ApiService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  get canEdit(): boolean {
    const raw = localStorage.getItem("rk_auth");

    if (!raw) {
      return false;
    }

    try {
      const roles = (JSON.parse(raw) as { roles?: string[] }).roles ?? [];
      return roles.includes("ADMIN") || roles.includes("EDITOR");
    } catch {
      return false;
    }
  }

  get contentJson(): string {
    return JSON.stringify(this.buildContent(), null, 2);
  }

  ngOnInit(): void {
    this.api.categories().subscribe((items) => {
      this.categories = items;
    });

    const id = this.route.snapshot.paramMap.get("id");

    if (id) {
      this.load(id);
    } else {
      this.blocks = [this.createBlock("paragraph")];
    }
  }

  load(id: string): void {
    this.api.getArticle(id).subscribe((article) => {
      this.article = article;
      this.form = {
        title: article.title,
        slug: article.slug,
        categoryId: article.categoryId ?? ""
      };
      this.visibility = article.visibility;

      this.api.getVersions(id).subscribe((versions) => {
        this.versions = versions;
        const active = versions.find((item) => item.id === article.activeVersionId) ?? versions[0];

        if (active) {
          this.selectVersion(active);
        }
      });
    });
  }

  selectVersion(version: ArticleVersion): void {
    this.version = version;
    this.blocks = this.readBlocks(version.content);
    this.changeSummary = version.changeSummary ?? "";
  }

  addBlock(type: ContentBlockType): void {
    this.blocks.push(this.createBlock(type));
  }

  removeBlock(index: number): void {
    this.blocks.splice(index, 1);
  }

  moveBlock(index: number, direction: -1 | 1): void {
    const target = index + direction;

    if (target < 0 || target >= this.blocks.length) {
      return;
    }

    const current = this.blocks[index];
    this.blocks[index] = this.blocks[target];
    this.blocks[target] = current;
  }

  addListItem(block: EditorBlock): void {
    block.items ??= [];
    block.items.push("");
  }

  removeListItem(block: EditorBlock, index: number): void {
    block.items?.splice(index, 1);
  }

  normalizeBlock(block: EditorBlock): void {
    if (block.type === "heading") {
      block.level ??= 2;
      block.text ??= "";
      delete block.items;
      delete block.url;
      delete block.alt;
      return;
    }

    if (block.type === "list") {
      block.items ??= [""];
      delete block.level;
      delete block.url;
      delete block.alt;
      return;
    }

    if (block.type === "image") {
      block.url ??= "";
      block.alt ??= "";
      delete block.level;
      delete block.text;
      delete block.items;
      return;
    }

    block.text ??= "";
    delete block.level;
    delete block.items;
    delete block.url;
    delete block.alt;
  }

  save(): void {
    this.message = "";
    this.error = "";

    const content = this.buildContent();

    if (content.blocks.length === 0) {
      this.error = "Add at least one content block.";
      return;
    }

    const hasInvalidHeading = content.blocks.some(
      (block) => block.type === "heading" && (!block.level || block.level < 1 || block.level > 4)
    );

    if (hasInvalidHeading) {
      this.error = "Heading levels must be between 1 and 4.";
      return;
    }

    this.saving = true;

    if (!this.article) {
      this.api.createArticle({
        title: this.form.title,
        slug: this.form.slug,
        categoryId: this.form.categoryId || null,
        content
      }).subscribe({
        next: (article) => {
          this.message = "Article created.";
          void this.router.navigate(["/articles", article.id]);
        },
        error: (error) => {
          this.error = error?.error?.message || "Article creation failed.";
          this.saving = false;
        },
        complete: () => {
          this.saving = false;
        }
      });

      return;
    }

    if (!this.version) {
      this.error = "No editable version is selected.";
      this.saving = false;
      return;
    }

    this.api.updateDraft(
      this.article.id,
      this.version.id,
      content,
      this.changeSummary
    ).subscribe({
      next: (version) => {
        this.version = version;
        this.message = "Draft saved.";
        this.load(this.article!.id);
      },
      error: (error) => {
        this.error = error?.error?.message || "Draft save failed.";
        this.saving = false;
      },
      complete: () => {
        this.saving = false;
      }
    });
  }

  setVisibility(): void {
    if (!this.article) {
      return;
    }

    this.api.setVisibility(this.article.id, this.visibility).subscribe({
      next: () => {
        this.message = "Visibility updated.";
      },
      error: (error) => {
        this.error = error?.error?.message || "Visibility update failed.";
      }
    });
  }

  newVersion(): void {
    if (!this.article) {
      return;
    }

    this.api.createDraftVersion(this.article.id).subscribe((version) => {
      this.versions = [version, ...this.versions];
      this.selectVersion(version);
      this.message = `Draft version ${version.versionNumber} created.`;
    });
  }

  submit(): void {
    if (!this.article || !this.version) {
      return;
    }

    this.api.submitForReview(this.article.id, this.version.id).subscribe((version) => {
      this.version = version;
      this.message = "Version submitted for review.";
      this.load(this.article!.id);
    });
  }

  archive(): void {
    if (!this.article || !confirm("Archive this article?")) {
      return;
    }

    this.api.archiveArticle(this.article.id).subscribe({
      next: () => {
        this.message = "Article archived.";
        this.load(this.article!.id);
      },
      error: (error) => {
        this.error = error?.error?.message || "Archive failed.";
      }
    });
  }

  uploadAsset(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];

    if (!file || !this.article) {
      return;
    }

    this.api.uploadAsset(file, this.article.id).subscribe({
      next: (asset) => {
        this.assetUrl = asset.downloadUrl;
        this.message = "Asset uploaded.";
      },
      error: (error) => {
        this.error = error?.error?.message || "Asset upload failed.";
      }
    });
  }

  private createBlock(type: ContentBlockType): EditorBlock {
    if (type === "heading") {
      return { type, level: 2, text: "" };
    }

    if (type === "list") {
      return { type, items: [""] };
    }

    if (type === "image") {
      return { type, url: "", alt: "" };
    }

    return { type, text: "" };
  }

  private readBlocks(content: unknown): EditorBlock[] {
    if (!content || typeof content !== "object" || Array.isArray(content)) {
      return [];
    }

    const blocks = (content as { blocks?: unknown }).blocks;

    if (!Array.isArray(blocks)) {
      return [];
    }

    return blocks
      .filter((block): block is Record<string, unknown> => !!block && typeof block === "object" && !Array.isArray(block))
      .map((block) => this.toEditorBlock(block))
      .filter((block): block is EditorBlock => block !== null);
  }

  private toEditorBlock(block: Record<string, unknown>): EditorBlock | null {
    const type = block["type"];

    if (type !== "heading" && type !== "paragraph" && type !== "image" && type !== "list" && type !== "code") {
      return null;
    }

    const editorBlock: EditorBlock = { type };

    if (type === "heading") {
      editorBlock.level = typeof block["level"] === "number" ? block["level"] : 2;
      editorBlock.text = typeof block["text"] === "string" ? block["text"] : "";
    } else if (type === "list") {
      editorBlock.items = Array.isArray(block["items"])
        ? block["items"].filter((item): item is string => typeof item === "string")
        : [typeof block["text"] === "string" ? block["text"] : ""];
    } else if (type === "image") {
      editorBlock.url = typeof block["url"] === "string"
        ? block["url"]
        : typeof block["src"] === "string"
          ? block["src"]
          : "";
      editorBlock.alt = typeof block["alt"] === "string" ? block["alt"] : "";
    } else {
      editorBlock.text = typeof block["text"] === "string" ? block["text"] : "";
    }

    return editorBlock;
  }

  private buildContent(): { blocks: EditorBlock[] } {
    return {
      blocks: this.blocks.map((block) => {
        if (block.type === "heading") {
          return {
            type: "heading",
            level: block.level ?? 2,
            text: block.text ?? ""
          };
        }

        if (block.type === "list") {
          return {
            type: "list",
            items: block.items ?? []
          };
        }

        if (block.type === "image") {
          return {
            type: "image",
            url: block.url ?? "",
            alt: block.alt ?? ""
          };
        }

        return {
          type: block.type,
          text: block.text ?? ""
        };
      })
    };
  }
}
