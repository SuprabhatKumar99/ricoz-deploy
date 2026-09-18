package com.ricozknow.asset;

import com.ricozknow.article.Article;
import com.ricozknow.article.ArticleRepository;
import com.ricozknow.audit.AuditService;
import com.ricozknow.common.TenantContext;
import com.ricozknow.user.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetService {

    // MVP allowlist: images used in article content, per spec section 8's "image" block type.
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp", "image/svg+xml");
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

    private final AssetRepository assetRepository;
    private final ArticleRepository articleRepository;
    private final AuditService auditService;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${storage.s3.bucket}")
    private String bucket;

    @Transactional
    public Asset upload(UUID articleId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalStateException("File is empty");
        }
        if (!ALLOWED_MIME_TYPES.contains(file.getContentType())) {
            throw new IllegalStateException("Unsupported file type: " + file.getContentType());
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalStateException("File exceeds maximum size of 10MB");
        }

        UUID tenantId = TenantContext.get();
        String storageKey = "%s/%s-%s".formatted(tenantId, UUID.randomUUID(), sanitizeFilename(file.getOriginalFilename()));

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(storageKey)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read uploaded file", ex);
        }

        Asset asset = new Asset();
        asset.setStorageKey(storageKey);
        asset.setFilename(file.getOriginalFilename());
        asset.setMimeType(file.getContentType());
        asset.setSizeBytes(file.getSize());
        asset.setCreatedBy(currentUserId());
        if (articleId != null) {
            Article article = articleRepository.findByTenantIdAndId(tenantId, articleId)
                    .orElseThrow(() -> new IllegalStateException("Unknown article: " + articleId));
            asset.setArticle(article);
        }
        asset = assetRepository.save(asset);

        auditService.record("Asset", asset.getId(), "ASSET_UPLOADED", null, asset.getFilename());
        return asset;
    }

    @Transactional(readOnly = true)
    public String presignedDownloadUrl(UUID assetId) {
        Asset asset = assetRepository.findByTenantIdAndId(TenantContext.get(), assetId)
                .orElseThrow(() -> new IllegalStateException("Asset not found: " + assetId));

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(asset.getStorageKey()).build())
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    @Transactional(readOnly = true)
    public Asset get(UUID assetId) {
        return assetRepository.findByTenantIdAndId(TenantContext.get(), assetId)
                .orElseThrow(() -> new IllegalStateException("Asset not found: " + assetId));
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "upload";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private UUID currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser principal) {
            return principal.userId();
        }
        throw new IllegalStateException("No authenticated user in context");
    }
}
