import { Injectable, inject } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import {
  Article,
  ArticlePerformance,
  ArticleVersion,
  Asset,
  Category,
  DeflectionSummary,
  KnowledgeGap,
  LoginRequest,
  LoginResponse,
  Page,
  PortalArticleDetail,
  PortalArticleSummary,
  RegisterRequest,
  RegisterResponse,
  SearchResult,
  StaleContent,
  Synonym,
  Tenant,
  User,
  Visibility
} from "../models";

@Injectable({ providedIn: "root" })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = (() => {
    const configured = localStorage.getItem("rk_api_url")?.replace(/\\/$/, "");
    if (configured) return configured;

    // Local development talks directly to Spring Boot.
    if (window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1") {
      return "http://localhost:8080";
    }

    // Render production frontend. Keep API traffic on HTTPS.
    return "https://ricozknow-api.onrender.com";
  })();

  private params(values: Record<string, string | number | boolean | undefined | null>): HttpParams {
    let params = new HttpParams();

    for (const [key, value] of Object.entries(values)) {
      if (value !== undefined && value !== null && value !== "") {
        params = params.set(key, String(value));
      }
    }

    return params;
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/api/v1/auth/login`, request, { withCredentials: true });
  }

  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.baseUrl}/api/v1/auth/register`, request);
  }

  refresh(): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/api/v1/auth/refresh`, {}, { withCredentials: true });
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/api/v1/auth/logout`, {}, { withCredentials: true });
  }

  tenantLookup(subdomain: string): Observable<Tenant> {
    return this.http.get<Tenant>(`${this.baseUrl}/api/v1/tenants/lookup`, {
      params: this.params({ subdomain })
    });
  }

  listArticles(status?: string, page = 0, size = 10): Observable<Page<Article>> {
    return this.http.get<Page<Article>>(`${this.baseUrl}/api/v1/articles`, {
      params: this.params({ status, page, size })
    });
  }

  getArticle(id: string): Observable<Article> {
    return this.http.get<Article>(`${this.baseUrl}/api/v1/articles/${id}`);
  }

  getVersions(articleId: string): Observable<ArticleVersion[]> {
    return this.http.get<ArticleVersion[]>(`${this.baseUrl}/api/v1/articles/${articleId}/versions`);
  }

  createArticle(request: {
    title: string;
    slug: string;
    categoryId: string | null;
    content: unknown;
  }): Observable<Article> {
    return this.http.post<Article>(`${this.baseUrl}/api/v1/articles`, request);
  }

  updateDraft(articleId: string, versionId: string, content: unknown, changeSummary: string): Observable<ArticleVersion> {
    return this.http.put<ArticleVersion>(
      `${this.baseUrl}/api/v1/articles/${articleId}/versions/${versionId}`,
      { content, changeSummary }
    );
  }

  createDraftVersion(articleId: string): Observable<ArticleVersion> {
    return this.http.post<ArticleVersion>(`${this.baseUrl}/api/v1/articles/${articleId}/versions`, {});
  }

  submitForReview(articleId: string, versionId: string): Observable<ArticleVersion> {
    return this.http.post<ArticleVersion>(
      `${this.baseUrl}/api/v1/articles/${articleId}/versions/${versionId}/submit`,
      {}
    );
  }

  reviewVersion(
    articleId: string,
    versionId: string,
    approve: boolean,
    comment: string
  ): Observable<ArticleVersion> {
    return this.http.post<ArticleVersion>(
      `${this.baseUrl}/api/v1/articles/${articleId}/versions/${versionId}/review`,
      { approve, comment }
    );
  }

  archiveArticle(articleId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/api/v1/articles/${articleId}/archive`, {});
  }

  setVisibility(articleId: string, visibility: Visibility): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/api/v1/articles/${articleId}/visibility`, null, {
      params: this.params({ visibility })
    });
  }

  categories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.baseUrl}/api/v1/categories`);
  }

  createCategory(request: {
    name: string;
    slug: string;
    parentId: string | null;
    sortOrder: number | null;
  }): Observable<Category> {
    return this.http.post<Category>(`${this.baseUrl}/api/v1/categories`, request);
  }

  updateCategory(id: string, request: {
    name: string;
    slug: string;
    parentId: string | null;
    sortOrder: number | null;
  }): Observable<Category> {
    return this.http.put<Category>(`${this.baseUrl}/api/v1/categories/${id}`, request);
  }

  deleteCategory(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/v1/categories/${id}`);
  }

  search(query: string, category?: string, page = 0): Observable<SearchResult> {
    return this.http.get<SearchResult>(`${this.baseUrl}/api/v1/search`, {
      params: this.params({ q: query, category, page })
    });
  }

  synonyms(): Observable<Synonym[]> {
    return this.http.get<Synonym[]>(`${this.baseUrl}/api/v1/admin/synonyms`);
  }

  upsertSynonym(term: string, synonyms: string[]): Observable<Synonym> {
    return this.http.post<Synonym>(`${this.baseUrl}/api/v1/admin/synonyms`, { term, synonyms });
  }

  deleteSynonym(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/v1/admin/synonyms/${id}`);
  }

  users(): Observable<User[]> {
    return this.http.get<User[]>(`${this.baseUrl}/api/v1/users`);
  }

  createUser(request: {
    email: string;
    name: string;
    password: string;
    roles: string[];
  }): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/api/v1/users`, request);
  }

  disableUser(id: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/api/v1/users/${id}/disable`, {});
  }

  uploadAsset(file: File, articleId?: string): Observable<Asset> {
    const body = new FormData();
    body.append("file", file);

    if (articleId) {
      body.append("articleId", articleId);
    }

    return this.http.post<Asset>(`${this.baseUrl}/api/v1/assets`, body);
  }

  performance(days = 30): Observable<ArticlePerformance[]> {
    return this.http.get<ArticlePerformance[]>(`${this.baseUrl}/api/v1/analytics/articles/performance`, {
      params: this.params({ days })
    });
  }

  knowledgeGaps(days = 30, limit = 20): Observable<KnowledgeGap[]> {
    return this.http.get<KnowledgeGap[]>(`${this.baseUrl}/api/v1/analytics/knowledge-gaps`, {
      params: this.params({ days, limit })
    });
  }

  deflection(days = 30): Observable<DeflectionSummary> {
    return this.http.get<DeflectionSummary>(`${this.baseUrl}/api/v1/analytics/deflection`, {
      params: this.params({ days })
    });
  }

  staleContent(horizonDays = 30): Observable<StaleContent[]> {
    return this.http.get<StaleContent[]>(`${this.baseUrl}/api/v1/analytics/stale-content`, {
      params: this.params({ horizonDays })
    });
  }

  aggregate(date?: string): Observable<{ status: string; date: string }> {
    return this.http.post<{ status: string; date: string }>(
      `${this.baseUrl}/api/v1/analytics/aggregate`,
      {},
      { params: this.params({ date }) }
    );
  }

  reindex(): Observable<{ indexed: number }> {
    return this.http.post<{ indexed: number }>(`${this.baseUrl}/api/v1/admin/search/reindex`, {});
  }

  updateBranding(request: {
    portalTitle: string;
    logoUrl: string;
    primaryColor: string;
  }): Observable<Tenant> {
    return this.http.put<Tenant>(`${this.baseUrl}/api/v1/admin/branding`, request);
  }

  // portalArticles(categoryId?: string, page = 0): Observable<Page<PortalArticleSummary>> {
  //   return this.http.get<Page<PortalArticleSummary>>(`${this.baseUrl}/api/v1/portal/articles`, {
  //     params: this.params({ categoryId, page })
  //   });
  // }

  // portalArticle(slug: string): Observable<PortalArticleDetail> {
  //   return this.http.get<PortalArticleDetail>(
  //     `${this.baseUrl}/api/v1/portal/articles/${encodeURIComponent(slug)}`
  //   );
  // }

  // portalSearch(query: string, categoryId?: string, page = 0): Observable<SearchResult> {
  //   return this.http.get<SearchResult>(`${this.baseUrl}/api/v1/portal/search`, {
  //     params: this.params({ q: query, categoryId, page })
  //   });
  // }

  // portalFeedback(articleId: string, helpful: boolean): Observable<void> {
  //   return this.http.post<void>(
  //     `${this.baseUrl}/api/v1/portal/articles/${articleId}/feedback`,
  //     null,
  //     { params: this.params({ helpful }) }
  //   );
  // }

  portalArticles(
    categoryId?: string,
    page = 0
  ): Observable<Page<PortalArticleSummary>> {
    return this.http.get<Page<PortalArticleSummary>>(
      `${this.baseUrl}/api/v1/portal/articles`,
      {
        params: this.params({
          categoryId,
          page,
          tenant: this.portalTenant()
        })
      }
    );
  }

  portalArticle(slug: string): Observable<PortalArticleDetail> {
    return this.http.get<PortalArticleDetail>(
      `${this.baseUrl}/api/v1/portal/articles/${encodeURIComponent(slug)}`,
      {
        params: this.params({
          tenant: this.portalTenant()
        })
      }
    );
  }

  portalSearch(
    query: string,
    categoryId?: string,
    page = 0
  ): Observable<SearchResult> {
    return this.http.get<SearchResult>(
      `${this.baseUrl}/api/v1/portal/search`,
      {
        params: this.params({
          q: query,
          categoryId,
          page,
          tenant: this.portalTenant()
        })
      }
    );
  }

  portalFeedback(
    articleId: string,
    helpful: boolean
  ): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl}/api/v1/portal/articles/${articleId}/feedback`,
      null,
      {
        params: this.params({
          helpful,
          tenant: this.portalTenant()
        })
      }
    );
  }

  agentSearch(query: string, category?: string, page = 0): Observable<SearchResult> {
    return this.http.get<SearchResult>(`${this.baseUrl}/api/v1/agent/search`, {
      params: this.params({ q: query, category, page })
    });
  }

  agentArticle(id: string): Observable<PortalArticleDetail> {
    return this.http.get<PortalArticleDetail>(`${this.baseUrl}/api/v1/agent/articles/${id}`);
  }


  getCurrentBranding(): Observable<Tenant> {
    return this.http.get<Tenant>(
      `${this.baseUrl}/api/v1/tenants/me`
    );
  }


  private portalTenant(): string | null {
    return new URLSearchParams(window.location.search).get("tenant");
  }

  getCurrentTenant(): Observable<Tenant> {
    return this.http.get<Tenant>(
      `${this.baseUrl}/api/v1/tenants/me`
    );
  }
}
