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
package org.jkiss.dbeaver.ext.cdata.ui;

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverLicense;
import org.jkiss.dbeaver.ext.cdata.registry.CDataLicenseStatus;
import org.jkiss.dbeaver.ext.cdata.ui.internal.CDataUIMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.UIIcon;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

final class CDataLicenseUIUtils {
    static final String SUPPORT_URL = "https://portal.cdata.com/?a=support";

    @NotNull
    static String getLicenseTitle(@NotNull CDataDriverLicense license) {
        Integer days = license.getRemainingDays();
        return switch (license.getStatus()) {
            case TRIAL_ACTIVE -> days == null
                ? getStatusText(license.getStatus()) : NLS.bind(CDataUIMessages.license_trial_remaining, days);
            case TRIAL_EXPIRING -> days == null
                ? getStatusText(license.getStatus()) : NLS.bind(CDataUIMessages.license_trial_expiring, days);
            case PURCHASED_EXPIRING -> days == null
                ? getStatusText(license.getStatus()) : NLS.bind(CDataUIMessages.license_purchased_expiring, days);
            default -> getStatusText(license.getStatus());
        };
    }

    @NotNull
    static String getLicenseDetails(@NotNull CDataDriverLicense license) {
        if (license.getStatus().isValid()) {
            var expiration = license.getExpirationDate();
            return expiration == null ? "" : NLS.bind(CDataUIMessages.license_valid_until,
                expiration.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.US)));
        }
        return switch (license.getStatus()) {
            case NOT_INSTALLED -> CDataUIMessages.license_required_description;
            case TRIAL_EXPIRED -> CDataUIMessages.license_trial_expired_description;
            case EXPIRED -> CDataUIMessages.license_expired_description;
            case VALIDATION_UNAVAILABLE -> CDataUIMessages.license_unverified_description;
            default -> CDataUIMessages.license_invalid_description;
        };
    }

    @Nullable
    static DBPImage getStatusIcon(@NotNull CDataLicenseStatus status) {
        return switch (status) {
            case PURCHASED_ACTIVE, TRIAL_ACTIVE -> UIIcon.ACCEPT;
            case PURCHASED_EXPIRING, TRIAL_EXPIRING, VALIDATION_UNAVAILABLE -> DBIcon.SMALL_WARNING;
            case NOT_INSTALLED -> null;
            default -> DBIcon.SMALL_ERROR;
        };
    }

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
