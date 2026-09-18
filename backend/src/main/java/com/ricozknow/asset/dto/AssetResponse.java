package com.ricozknow.asset.dto;

import com.ricozknow.asset.Asset;

import java.util.UUID;

public record AssetResponse(
        UUID id,
        String filename,
        String mimeType,
        long sizeBytes,
        String downloadUrl
) {
    public static AssetResponse from(Asset asset, String downloadUrl) {
        return new AssetResponse(asset.getId(), asset.getFilename(), asset.getMimeType(), asset.getSizeBytes(), downloadUrl);
    }
}
