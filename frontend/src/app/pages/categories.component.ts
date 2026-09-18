import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ApiService } from "../core/api.service";
import { Category } from "../models";

@Component({
  selector: "rk-categories",
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div>
      <div>
        <p class="text-sm font-semibold text-slate-500">Knowledge Studio</p>
        <h1 class="mt-1 text-3xl font-bold tracking-tight">Categories</h1>
        <p class="mt-2 text-sm text-slate-500">Manage the taxonomy exposed by the backend.</p>
      </div>

      <div class="mt-7 grid gap-6 xl:grid-cols-[360px_1fr]">
        <form class="rk-card p-6" (ngSubmit)="save()">
          <h2 class="font-bold">{{ editingId ? "Edit category" : "New category" }}</h2>

          <div class="mt-5 space-y-4">
            <div>
              <label class="rk-label" for="name">Name</label>
              <input id="name" class="rk-input" [(ngModel)]="form.name" name="name" required />
            </div>

            <div>
              <label class="rk-label" for="slug">Slug</label>
              <input id="slug" class="rk-input" [(ngModel)]="form.slug" name="slug" required />
            </div>

            <div>
              <label class="rk-label" for="parent">Parent</label>
              <select id="parent" class="rk-select" [(ngModel)]="form.parentId" name="parent">
                <option value="">No parent</option>
                <option *ngFor="let category of list" [value]="category.id">
                  {{ category.name }}
                </option>
              </select>
            </div>

            <div>
              <label class="rk-label" for="sortOrder">Sort order</label>
              <input id="sortOrder" class="rk-input" type="number" [(ngModel)]="form.sortOrder" name="sortOrder" />
            </div>

            <button class="rk-btn rk-btn-primary w-full">
              {{ editingId ? "Update category" : "Create category" }}
            </button>

            <button
              *ngIf="editingId"
              type="button"
              class="rk-btn rk-btn-secondary w-full"
              (click)="reset()"
            >
              Cancel
            </button>
          </div>
        </form>

        <section class="rk-card overflow-hidden">
          <div class="overflow-x-auto">
            <table class="w-full min-w-[620px] text-left text-sm">
              <thead class="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th class="px-5 py-3">Name</th>
                  <th class="px-5 py-3">Slug</th>
                  <th class="px-5 py-3">Parent</th>
                  <th class="px-5 py-3">Order</th>
                  <th class="px-5 py-3"></th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                <tr *ngFor="let category of list">
                  <td class="px-5 py-4 font-semibold">{{ category.name }}</td>
                  <td class="px-5 py-4 text-slate-500">{{ category.slug }}</td>
                  <td class="px-5 py-4 text-slate-500">{{ parentName(category.parentId) }}</td>
                  <td class="px-5 py-4">{{ category.sortOrder }}</td>
                  <td class="px-5 py-4 text-right">
                    <button class="rk-btn rk-btn-secondary mr-2" (click)="edit(category)">Edit</button>
                    <button class="rk-btn rk-btn-danger" (click)="remove(category)">Delete</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>
  `
})
export class CategoriesComponent implements OnInit {
  list: Category[] = [];
  editingId = "";

  form = {
    name: "",
    slug: "",
    parentId: "",
    sortOrder: 0
  };

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.categories().subscribe((items) => {
      this.list = items;
    });
  }

  edit(category: Category): void {
    this.editingId = category.id;
    this.form = {
      name: category.name,
      slug: category.slug,
      parentId: category.parentId ?? "",
      sortOrder: category.sortOrder
    };
  }

  reset(): void {
    this.editingId = "";
    this.form = {
      name: "",
      slug: "",
      parentId: "",
      sortOrder: 0
    };
  }

  save(): void {
    const request = {
      name: this.form.name,
      slug: this.form.slug,
      parentId: this.form.parentId || null,
      sortOrder: this.form.sortOrder
    };

    const operation = this.editingId
      ? this.api.updateCategory(this.editingId, request)
      : this.api.createCategory(request);

    operation.subscribe(() => {
      this.reset();
      this.load();
    });
  }

  remove(category: Category): void {
    if (!confirm(`Delete ${category.name}?`)) {
      return;
    }

    this.api.deleteCategory(category.id).subscribe(() => this.load());
  }

  parentName(id: string | null): string {
    return this.list.find((category) => category.id === id)?.name ?? "—";
  }
}
