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
import org.jkiss.dbeaver.model.connection.DBPDriverLicense;

public final class CDataDriverLicense implements DBPDriverLicense {
    private final CDataLicenseStatus status;
    private final String licenseId;
    private final String message;

    public CDataDriverLicense(
        @NotNull CDataLicenseStatus status,
        @Nullable String licenseId,
        @Nullable String message
    ) {
        this.status = status;
        this.licenseId = licenseId == null ? "" : licenseId;
        this.message = message;
    }

    @NotNull
    public CDataLicenseStatus getStatus() {
        return status;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @NotNull
    @Override
    public String getLicenseId() {
        return licenseId;
    }

    @Override
    public boolean isValidLicense() {
        return status.isValid();
    }

    @Override
    public boolean isTrialLicense() {
        return status.isTrial();
    }
}
