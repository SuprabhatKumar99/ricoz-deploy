export type Role = "ADMIN" | "EDITOR" | "REVIEWER" | "AGENT_VIEWER";
export type ArticleStatus = "DRAFT" | "IN_REVIEW" | "PUBLISHED" | "ARCHIVED";
export type VersionStatus = "DRAFT" | "IN_REVIEW" | "REJECTED" | "PUBLISHED";
export type Visibility = "PUBLIC" | "AUTHENTICATED" | "PRIVATE";

export interface LoginRequest {
  tenantSlug: string;
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  userId: string;
  tenantId: string;
  email: string;
  roles: string[];
}

export interface RegisterRequest {
  organizationName: string;
  tenantSlug: string;
  subdomain: string;
  email: string;
  password: string;
  name: string;
}

export interface RegisterResponse {
  tenantId: string;
  userId: string;
  tenantSlug: string;
  email: string;
  role: string;
  message: string;
}

export interface Tenant {
  tenantId: string;
  slug: string;
  name: string;
  portalTitle: string;
  logoUrl: string;
  primaryColor: string;
}

export interface Category {
  id: string;
  name: string;
  slug: string;
  parentId: string | null;
  sortOrder: number;
}

export interface Article {
  id: string;
  slug: string;
  title: string;
  status: ArticleStatus;
  visibility: Visibility;
  categoryId: string | null;
  activeVersionId: string | null;
  activeVersionNumber: number | null;
}

export interface ArticleVersion {
  id: string;
  versionNumber: number;
  content: unknown;
  changeSummary: string | null;
  status: VersionStatus;
  createdBy: string;
  reviewedBy: string | null;
  reviewedAt: string | null;
  publishedAt: string | null;
  expiresAt: string | null;
}

export interface SearchHit {
  articleId: string;
  slug: string;
  title: string;
  excerpt: string;
  categoryId: string | null;
  score: number;
}

export interface SearchResult {
  query: string;
  results: SearchHit[];
  total: number;
}

export interface PortalArticleSummary {
  id: string;
  slug: string;
  title: string;
  categoryId: string | null;
}

export interface PortalArticleDetail {
  id: string;
  slug: string;
  title: string;
  categoryId: string | null;
  visibility: Visibility;
  content: RichTextContent;
  versionNumber: number;
  publishedAt: string;
}

export interface User {
  id: string;
  email: string;
  name: string;
  status: string;
  roles: string[];
}

export interface Synonym {
  id: string;
  term: string;
  synonyms: string[];
}

export interface Asset {
  id: string;
  filename: string;
  mimeType: string;
  sizeBytes: number;
  downloadUrl: string;
}

export interface ArticlePerformance {
  articleId: string;
  title: string;
  views: number;
  helpfulCount: number;
  unhelpfulCount: number;
}

export interface KnowledgeGap {
  query: string;
  volumeCount: number;
  zeroResultCount: number;
  unsuccessfulCount: number;
  gapScore: number;
}

export interface DeflectionSummary {
  estimatedDeflectionCount: number;
  confirmedDeflectionCount: number;
  supportContactCount: number;
}

export interface StaleContent {
  articleId: string;
  versionId: string;
  title: string;
  expiresAt: string;
  alreadyExpired: boolean;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

// export interface RichTextBlock {
//   [key: string]: unknown;
// }

// export interface RichTextContent {
//   blocks: RichTextBlock[];
// }

export interface RichTextBlock {
  type: "heading" | "paragraph" | "list" | "code" | "image";
  text?: string;
  level?: number;
  items?: string[];
  code?: string;
  url?: string;
  alt?: string;
  caption?: string;
}

export interface RichTextContent {
  blocks: RichTextBlock[];
}