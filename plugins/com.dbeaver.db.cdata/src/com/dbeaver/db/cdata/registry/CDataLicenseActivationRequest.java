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
package com.dbeaver.db.cdata.registry;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public record CDataLicenseActivationRequest(
    @NotNull String name,
    @NotNull String email,
    @NotNull CDataLicenseType type,
    @Nullable String productKey
) {
    public CDataLicenseActivationRequest {
        if (name.isBlank() || email.isBlank()) {
            throw new IllegalArgumentException("Name and email are required");
        }
        if (type == CDataLicenseType.PURCHASED && (productKey == null || productKey.isBlank())) {
            throw new IllegalArgumentException("Product key is required for purchased activation");
        }
        if (type == CDataLicenseType.TRIAL) {
            productKey = null;
        }
    }
}
