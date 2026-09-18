// package com.ricozknow.asset;

// import com.ricozknow.asset.dto.AssetResponse;
// import lombok.RequiredArgsConstructor;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;
// import org.springframework.web.multipart.MultipartFile;

// import java.util.UUID;

// @RestController
// @RequestMapping("/api/v1/assets")
// @RequiredArgsConstructor
// public class AssetController {

//     private final AssetService assetService;

//     @PostMapping(consumes = "multipart/form-data")
//     @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
//     public ResponseEntity<AssetResponse> upload(@RequestParam(required = false) UUID articleId,
//                                                  @RequestParam("file") MultipartFile file) {
//         Asset asset = assetService.upload(articleId, file);
//         String url = assetService.presignedDownloadUrl(asset.getId());
//         return ResponseEntity.ok(AssetResponse.from(asset, url));
//     }

//     @GetMapping("/{id}")
//     public AssetResponse get(@PathVariable UUID id) {
//         Asset asset = assetService.get(id);
//         return AssetResponse.from(asset, assetService.presignedDownloadUrl(id));
//     }
// }


package com.ricozknow.asset;

import com.ricozknow.asset.dto.AssetResponse;
import com.ricozknow.common.RateLimiter;
import com.ricozknow.common.TooManyRequestsException;
import com.ricozknow.user.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final RateLimiter rateLimiter;

    @Value("${ricozknow.security.rate-limits.asset-upload.max-requests}")
    private int uploadMaxRequests;
    @Value("${ricozknow.security.rate-limits.asset-upload.window-seconds}")
    private long uploadWindowSeconds;

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<AssetResponse> upload(@RequestParam(required = false) UUID articleId,
                                                 @RequestParam("file") MultipartFile file) {
        String key = "asset-upload:" + currentUserId();
        if (!rateLimiter.allow(key, uploadMaxRequests, Duration.ofSeconds(uploadWindowSeconds))) {
            throw new TooManyRequestsException("Too many uploads. Please slow down.");
        }
        Asset asset = assetService.upload(articleId, file);
        String url = assetService.presignedDownloadUrl(asset.getId());
        return ResponseEntity.ok(AssetResponse.from(asset, url));
    }

    @GetMapping("/{id}")
    public AssetResponse get(@PathVariable UUID id) {
        Asset asset = assetService.get(id);
        return AssetResponse.from(asset, assetService.presignedDownloadUrl(id));
    }

    private UUID currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser principal) {
            return principal.userId();
        }
        throw new IllegalStateException("No authenticated user in context");
    }
}