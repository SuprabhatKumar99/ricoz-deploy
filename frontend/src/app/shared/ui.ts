import { CommonModule } from "@angular/common";
import { Component, Input } from "@angular/core";
import { RouterLink } from "@angular/router";

@Component({
  selector: "rk-metric-card",
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="rk-card p-5">
      <p class="text-sm font-medium text-slate-500">{{ label }}</p>
      <div class="mt-2 flex items-end justify-between gap-3">
        <p class="text-3xl font-bold tracking-tight">{{ value }}</p>
        <span *ngIf="hint" class="text-xs text-slate-400">{{ hint }}</span>
      </div>
    </div>
  `
})
export class MetricCardComponent {
  @Input() label = "";
  @Input() value: string | number = "—";
  @Input() hint = "";
}

@Component({
  selector: "rk-status",
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="rk-badge" [ngClass]="badgeClass">
      {{ status }}
    </span>
  `
})
export class StatusComponent {
  @Input() status = "";

  get badgeClass(): string {
    switch (this.status) {
      case "PUBLISHED":
        return "bg-emerald-100 text-emerald-700";
      case "IN_REVIEW":
        return "bg-amber-100 text-amber-700";
      case "DRAFT":
        return "bg-slate-100 text-slate-700";
      case "ARCHIVED":
        return "bg-slate-200 text-slate-600";
      case "REJECTED":
        return "bg-rose-100 text-rose-700";
      default:
        return "bg-slate-100 text-slate-700";
    }
  }
}

@Component({
  selector: "rk-empty",
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="rk-card px-6 py-12 text-center">
      <div class="mx-auto grid h-11 w-11 place-items-center rounded-xl bg-slate-100 text-slate-500">⌕</div>
      <h3 class="mt-4 text-lg font-bold">{{ title }}</h3>
      <p class="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-500">{{ message }}</p>
      <a *ngIf="link" [routerLink]="link" class="rk-btn rk-btn-primary mt-5">
        {{ action }}
      </a>
    </div>
  `
})
export class EmptyStateComponent {
  @Input() title = "";
  @Input() message = "";
  @Input() action = "";
  @Input() link: string | undefined;
}
