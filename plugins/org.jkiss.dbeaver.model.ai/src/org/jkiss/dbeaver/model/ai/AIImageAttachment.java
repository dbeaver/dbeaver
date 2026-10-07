/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.ai;

import org.jkiss.code.NotNull;

import java.util.Base64;
import java.util.List;
import java.util.Set;

public record AIImageAttachment(@NotNull String name, @NotNull String mediaType, @NotNull String data) {
    public static final int MAX_IMAGE_BYTES = 20 * 1024 * 1024;
    public static final int MAX_IMAGES = 10;
    public static final int DEFAULT_TOKEN_ESTIMATE = 4096;
    private static final Set<String> MEDIA_TYPES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp");

    public AIImageAttachment {
        if (!MEDIA_TYPES.contains(mediaType)) {
            throw new IllegalArgumentException("Unsupported image format. Use PNG, JPEG, GIF or WebP.");
        }
        if (data.isEmpty() || data.length() > (MAX_IMAGE_BYTES + 2) / 3 * 4 || getByteSize(data) > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Image must contain data and be no larger than 20 MB.");
        }
    }

    public int getByteSize() {
        return getByteSize(data);
    }

    private static int getByteSize(@NotNull String data) {
        int padding = data.endsWith("==") ? 2 : data.endsWith("=") ? 1 : 0;
        return data.length() * 3 / 4 - padding;
    }

    public static void validateImages(@NotNull List<AIImageAttachment> images) {
        if (images.size() > MAX_IMAGES || images.stream().mapToLong(AIImageAttachment::getByteSize).sum() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Attach no more than 10 images with a total size of 20 MB or less.");
        }
    }

    @Override
    @NotNull
    public String toString() {
        return name + " (" + mediaType + ")";
    }

    @NotNull
    public String toDataUrl() {
        return "data:" + mediaType + ";base64," + data;
    }

    @NotNull
    public byte[] getBytes() {
        return Base64.getDecoder().decode(data);
    }

    @NotNull
    public static AIImageAttachment fromBytes(@NotNull String name, @NotNull byte[] bytes) {
        if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Image must contain data and be no larger than 20 MB.");
        }
        String mediaType;
        if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
            && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) {
            mediaType = "image/png";
        } else if (bytes.length >= 3 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8 && bytes[2] == (byte) 0xff) {
            mediaType = "image/jpeg";
        } else if (bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F'
            && bytes[3] == '8' && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a') {
            mediaType = "image/gif";
        } else if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
            && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            mediaType = "image/webp";
        } else {
            throw new IllegalArgumentException("Unsupported image format. Use PNG, JPEG, GIF or WebP.");
        }
        return new AIImageAttachment(name, mediaType, Base64.getEncoder().encodeToString(bytes));
    }
}
