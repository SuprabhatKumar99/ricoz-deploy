import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ApiService } from "../core/api.service";
import { User } from "../models";

@Component({
  selector: "rk-users",
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div>
      <p class="text-sm font-semibold text-slate-500">Administration</p>
      <h1 class="mt-1 text-3xl font-bold tracking-tight">Users</h1>
      <p class="mt-2 text-sm text-slate-500">Manage tenant users and their backend roles.</p>

      <div class="mt-7 grid gap-6 xl:grid-cols-[380px_1fr]">
        <form class="rk-card p-6" (ngSubmit)="create()">
          <h2 class="font-bold">Create user</h2>

          <div class="mt-5 space-y-4">
            <div>
              <label class="rk-label" for="name">Name</label>
              <input id="name" class="rk-input" [(ngModel)]="form.name" name="name" required />
            </div>

            <div>
              <label class="rk-label" for="email">Email</label>
              <input id="email" class="rk-input" type="email" [(ngModel)]="form.email" name="email" required />
            </div>

            <div>
              <label class="rk-label" for="password">Password</label>
              <input id="password" class="rk-input" type="password" [(ngModel)]="form.password" name="password" minlength="12" required />
            </div>

            <fieldset>
              <legend class="rk-label">Roles</legend>
              <div class="grid grid-cols-2 gap-2">
                <label *ngFor="let role of roles" class="flex cursor-pointer items-center gap-2 rounded-xl border border-slate-200 p-3 text-sm">
                  <input
                    type="checkbox"
                    [checked]="form.roles.includes(role)"
                    (change)="toggleRole(role)"
                  />
                  {{ role }}
                </label>
              </div>
            </fieldset>

            <button class="rk-btn rk-btn-primary w-full">Create user</button>
          </div>
        </form>

        <section class="rk-card overflow-hidden">
          <div class="overflow-x-auto">
            <table class="w-full min-w-[720px] text-left text-sm">
              <thead class="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th class="px-5 py-3">User</th>
                  <th class="px-5 py-3">Status</th>
                  <th class="px-5 py-3">Roles</th>
                  <th class="px-5 py-3"></th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                <tr *ngFor="let user of users">
                  <td class="px-5 py-4">
                    <p class="font-semibold">{{ user.name }}</p>
                    <p class="mt-1 text-xs text-slate-500">{{ user.email }}</p>
                  </td>
                  <td class="px-5 py-4">{{ user.status }}</td>
                  <td class="px-5 py-4 text-slate-600">{{ user.roles.join(", ") }}</td>
                  <td class="px-5 py-4 text-right">
                    <button
                      *ngIf="user.status === 'ACTIVE'"
                      class="rk-btn rk-btn-danger"
                      (click)="disable(user)"
                    >
                      Disable
                    </button>
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
export class UsersComponent implements OnInit {
  readonly roles = ["ADMIN", "EDITOR", "REVIEWER", "AGENT_VIEWER"];
  users: User[] = [];

  form = {
    name: "",
    email: "",
    password: "",
    roles: [] as string[]
  };

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.users().subscribe((items) => {
      this.users = items;
    });
  }

  toggleRole(role: string): void {
    this.form.roles = this.form.roles.includes(role)
      ? this.form.roles.filter((item) => item !== role)
      : [...this.form.roles, role];
  }

  create(): void {
    this.api.createUser(this.form).subscribe(() => {
      this.form = {
        name: "",
        email: "",
        password: "",
        roles: []
      };
      this.load();
    });
  }

  disable(user: User): void {
    if (!confirm(`Disable ${user.email}?`)) {
      return;
    }

    this.api.disableUser(user.id).subscribe(() => this.load());
  }
}
