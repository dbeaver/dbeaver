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
package com.dbeaver.db.cdata.ui;

import com.dbeaver.db.cdata.registry.CDataLicenseStatus;
import com.dbeaver.db.cdata.ui.internal.CDataUIMessages;
import org.jkiss.code.NotNull;

final class CDataLicenseUIUtils {
    private CDataLicenseUIUtils() {
    }

    @NotNull
    static String getStatusText(@NotNull CDataLicenseStatus status) {
        return switch (status) {
            case NOT_INSTALLED -> CDataUIMessages.license_status_not_installed;
            case TRIAL_ACTIVE -> CDataUIMessages.license_status_trial_active;
            case TRIAL_EXPIRING -> CDataUIMessages.license_status_trial_expiring;
            case TRIAL_EXPIRED -> CDataUIMessages.license_status_trial_expired;
            case PURCHASED_ACTIVE -> CDataUIMessages.license_status_purchased_active;
            case PURCHASED_EXPIRING -> CDataUIMessages.license_status_purchased_expiring;
            case EXPIRED -> CDataUIMessages.license_status_expired;
            case INVALID_KEY -> CDataUIMessages.license_status_invalid_key;
            case MACHINE_MISMATCH -> CDataUIMessages.license_status_machine_mismatch;
            case WRONG_MAJOR_VERSION -> CDataUIMessages.license_status_wrong_major;
            case VALIDATION_UNAVAILABLE -> CDataUIMessages.license_status_validation_unavailable;
        };
    }
}
