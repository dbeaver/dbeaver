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
package org.jkiss.dbeaver.ext.cdata.registry;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.regex.Pattern;

public record CDataLicenseActivationRequest(
    @NotNull String name,
    @NotNull String email,
    @NotNull CDataLicenseType type,
    @Nullable String productKey
) {
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_EMAIL_LOCAL_PART_LENGTH = 64;
    private static final String EMAIL_LOCAL_PART = "[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+";
    private static final String EMAIL_DOMAIN_LABEL = "[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        EMAIL_LOCAL_PART + "(?:\\." + EMAIL_LOCAL_PART + ")*@" + EMAIL_DOMAIN_LABEL + "(?:\\." + EMAIL_DOMAIN_LABEL + ")+"
    );

    public CDataLicenseActivationRequest {
        if (name.isBlank() || email.isBlank()) {
            throw new IllegalArgumentException("Name and email are required");
        }
        email = email.strip();
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Email is invalid");
        }
        if (type == CDataLicenseType.PURCHASED && (productKey == null || productKey.isBlank())) {
            throw new IllegalArgumentException("Product key is required for purchased activation");
        }
        if (type == CDataLicenseType.TRIAL) {
            productKey = null;
        }
    }

    public static boolean isValidEmail(@NotNull String email) {
        return email.length() <= MAX_EMAIL_LENGTH
            && email.indexOf('@') <= MAX_EMAIL_LOCAL_PART_LENGTH
            && EMAIL_PATTERN.matcher(email).matches();
    }
}
