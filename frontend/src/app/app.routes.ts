import { Routes } from "@angular/router";
import { authGuard, roleGuard } from "./core/guards";

export const routes: Routes = [
  {
    path: "login",
    loadComponent: () => import("./pages/login.component").then((m) => m.LoginComponent)
  },
  {
    path: "register",
    loadComponent: () => import("./pages/register.component").then((m) => m.RegisterComponent)
  },
  {
    path: "portal",
    loadComponent: () => import("./pages/portal.component").then((m) => m.PortalComponent)
  },
  {
    path: "portal/search",
    loadComponent: () => import("./pages/portal-search.component").then((m) => m.PortalSearchComponent)
  },
  {
    path: "portal/article/:slug",
    loadComponent: () => import("./pages/portal-article.component").then((m) => m.PortalArticleComponent)
  },
  {
    path: "",
    canActivate: [authGuard],
    loadComponent: () => import("./pages/shell.component").then((m) => m.ShellComponent),
    children: [
      {
        path: "dashboard",
        loadComponent: () => import("./pages/dashboard.component").then((m) => m.DashboardComponent)
      },
      {
        path: "articles",
        loadComponent: () => import("./pages/articles.component").then((m) => m.ArticlesComponent)
      },
      {
        path: "articles/new",
        canActivate: [roleGuard(["ADMIN", "EDITOR"])],
        loadComponent: () => import("./pages/article-editor.component").then((m) => m.ArticleEditorComponent)
      },
      {
        path: "articles/:id",
        loadComponent: () => import("./pages/article-editor.component").then((m) => m.ArticleEditorComponent)
      },
      {
        path: "review",
        canActivate: [roleGuard(["ADMIN", "REVIEWER"])],
        loadComponent: () => import("./pages/review.component").then((m) => m.ReviewComponent)
      },
      {
        path: "categories",
        loadComponent: () => import("./pages/categories.component").then((m) => m.CategoriesComponent)
      },
      {
        path: "analytics",
        canActivate: [roleGuard(["ADMIN"])],
        loadComponent: () => import("./pages/analytics.component").then((m) => m.AnalyticsComponent)
      },
      {
        path: "users",
        canActivate: [roleGuard(["ADMIN"])],
        loadComponent: () => import("./pages/users.component").then((m) => m.UsersComponent)
      },
      {
        path: "branding",
        canActivate: [roleGuard(["ADMIN"])],
        loadComponent: () => import("./pages/branding.component").then((m) => m.BrandingComponent)
      },
      {
        path: "synonyms",
        canActivate: [roleGuard(["ADMIN"])],
        loadComponent: () => import("./pages/synonyms.component").then((m) => m.SynonymsComponent)
      },
      {
        path: "admin-tools",
        canActivate: [roleGuard(["ADMIN"])],
        loadComponent: () => import("./pages/admin-tools.component").then((m) => m.AdminToolsComponent)
      },
      {
        path: "agent",
        canActivate: [roleGuard(["ADMIN", "AGENT_VIEWER"])],
        loadComponent: () => import("./pages/agent.component").then((m) => m.AgentComponent)
      },
      {
        path: "",
        pathMatch: "full",
        redirectTo: "dashboard"
      }
    ]
  },
  {
    path: "**",
    redirectTo: "portal"
  }
];
