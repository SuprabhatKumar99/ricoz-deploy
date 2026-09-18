import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ApiService } from "../core/api.service";
import { Synonym } from "../models";

@Component({
  selector: "rk-synonyms",
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div>
      <p class="text-sm font-semibold text-slate-500">Administration</p>
      <h1 class="mt-1 text-3xl font-bold tracking-tight">Search synonyms</h1>
      <p class="mt-2 text-sm text-slate-500">Manage the exact synonym API exposed by the backend.</p>

      <div class="mt-7 grid gap-6 xl:grid-cols-[380px_1fr]">
        <form class="rk-card p-6" (ngSubmit)="save()">
          <h2 class="font-bold">Add or update</h2>

          <div class="mt-5 space-y-4">
            <div>
              <label class="rk-label" for="term">Term</label>
              <input id="term" class="rk-input" [(ngModel)]="term" name="term" required />
            </div>

            <div>
              <label class="rk-label" for="synonyms">Alternatives</label>
              <textarea
                id="synonyms"
                class="rk-textarea min-h-32"
                [(ngModel)]="rawSynonyms"
                name="synonyms"
                placeholder="One synonym per line"
                required
              ></textarea>
            </div>

            <button class="rk-btn rk-btn-primary w-full">Save synonym</button>
          </div>
        </form>

        <section class="rk-card overflow-hidden">
          <div class="overflow-x-auto">
            <table class="w-full min-w-[620px] text-left text-sm">
              <thead class="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th class="px-5 py-3">Term</th>
                  <th class="px-5 py-3">Alternatives</th>
                  <th class="px-5 py-3"></th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                <tr *ngFor="let item of list">
                  <td class="px-5 py-4 font-semibold">{{ item.term }}</td>
                  <td class="px-5 py-4 text-slate-600">{{ item.synonyms.join(", ") }}</td>
                  <td class="px-5 py-4 text-right">
                    <button class="rk-btn rk-btn-danger" (click)="remove(item)">Delete</button>
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
export class SynonymsComponent implements OnInit {
  list: Synonym[] = [];
  term = "";
  rawSynonyms = "";

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.synonyms().subscribe((items) => {
      this.list = items;
    });
  }

  save(): void {
    const synonyms = this.rawSynonyms
      .split("\n")
      .map((item) => item.trim())
      .filter(Boolean);

    this.api.upsertSynonym(this.term.trim(), synonyms).subscribe(() => {
      this.term = "";
      this.rawSynonyms = "";
      this.load();
    });
  }

  remove(item: Synonym): void {
    this.api.deleteSynonym(item.id).subscribe(() => this.load());
  }
}
