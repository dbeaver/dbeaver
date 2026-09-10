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

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CDataLicenseParser {
    private static final Pattern DAYS_REMAINING = Pattern.compile(
        "(?:(?:expires?|expiration)\\D{0,20}(\\d+)\\s+days?|(\\d+)\\s+days?\\s+(?:left|remaining))"
    );
    private static final Pattern ACTIVATION_ERROR = Pattern.compile(
        "Error validating user input:\\s*(.+?)\\s*(?:\\[code:|\\.(?:\\s|$))",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ACTIVE_WORD = Pattern.compile("\\bactive\\b");
    private static final Pattern VALID_WORD = Pattern.compile("\\bvalid\\b");
    private static final Pattern INVALID_WORD = Pattern.compile("\\binvalid\\b");
    private static final Pattern EXPIRED_CODE = Pattern.compile("\\[code:\\s*j(?:\\s|\\])");

    private CDataLicenseParser() {
    }

    @NotNull
    static CDataDriverLicense parseInformation(@NotNull Map<?, ?> information) {
        if (!information.containsKey("License")) {
            return new CDataDriverLicense(CDataLicenseStatus.VALIDATION_UNAVAILABLE, "", null);
        }
        String license = getValue(information, "License");
        String nodeId = getValue(information, "NodeId");
        String normalized = license.toLowerCase(Locale.ENGLISH);
        if (license.isBlank() || normalized.contains("no license")) {
            return new CDataDriverLicense(CDataLicenseStatus.NOT_INSTALLED, nodeId, null);
        }
        CDataLicenseStatus errorStatus = parseErrorStatus(normalized);
        if (errorStatus != null) {
            return new CDataDriverLicense(errorStatus, nodeId, null);
        }
        if (hasUnavailableStatus(normalized)) {
            return new CDataDriverLicense(CDataLicenseStatus.VALIDATION_UNAVAILABLE, nodeId, null);
        }

        boolean trial = isTrial(normalized);
        Integer remainingDays = getRemainingDays(normalized);
        boolean expiring = remainingDays == null ? normalized.contains("expiring") : remainingDays <= 3;
        if (!trial && (normalized.contains("trial") || !isPurchased(normalized, expiring))) {
            return new CDataDriverLicense(CDataLicenseStatus.VALIDATION_UNAVAILABLE, nodeId, null);
        }
        CDataLicenseStatus status = trial
            ? expiring ? CDataLicenseStatus.TRIAL_EXPIRING : CDataLicenseStatus.TRIAL_ACTIVE
            : expiring ? CDataLicenseStatus.PURCHASED_EXPIRING : CDataLicenseStatus.PURCHASED_ACTIVE;
        return new CDataDriverLicense(status, nodeId, null, remainingDays);
    }

    @Nullable
    static String parseActivationMessage(@NotNull String output) {
        Matcher matcher = ACTIVATION_ERROR.matcher(output.replaceAll("\\s+", " "));
        return matcher.find() ? matcher.group(1) : null;
    }

    @NotNull
    static CDataLicenseStatus parseActivationFailure(@NotNull String output) {
        CDataLicenseStatus errorStatus = parseErrorStatus(output.toLowerCase(Locale.ENGLISH));
        return errorStatus == null ? CDataLicenseStatus.VALIDATION_UNAVAILABLE : errorStatus;
    }

    @Nullable
    static CDataLicenseStatus parseExpiredLicenseError(@NotNull Throwable error) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        var errors = new ArrayDeque<Throwable>();
        errors.add(error);
        while (!errors.isEmpty()) {
            Throwable current = errors.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ENGLISH);
                if ((normalized.contains("license") && normalized.contains("expired")) ||
                    EXPIRED_CODE.matcher(normalized).find()) {
                    return normalized.contains("trial") ? CDataLicenseStatus.TRIAL_EXPIRED : CDataLicenseStatus.EXPIRED;
                }
            }
            if (current.getCause() != null) {
                errors.add(current.getCause());
            }
            if (current instanceof SQLException sqlException && sqlException.getNextException() != null) {
                errors.add(sqlException.getNextException());
            }
        }
        return null;
    }

    @Nullable
    private static CDataLicenseStatus parseErrorStatus(@NotNull String normalized) {
        if (normalized.contains("machine mismatch") || normalized.contains("different machine") ||
            normalized.contains("node mismatch")) {
            return CDataLicenseStatus.MACHINE_MISMATCH;
        }
        if (normalized.contains("wrong major") || normalized.contains("different major") ||
            normalized.contains("version mismatch")) {
            return CDataLicenseStatus.WRONG_MAJOR_VERSION;
        }
        if (normalized.contains("expired")) {
            return normalized.contains("trial") ? CDataLicenseStatus.TRIAL_EXPIRED : CDataLicenseStatus.EXPIRED;
        }
        if (normalized.contains("invalid product key") ||
            (INVALID_WORD.matcher(normalized).find() && normalized.contains("license")) ||
            normalized.contains("[code: c")) {
            return CDataLicenseStatus.INVALID_KEY;
        }
        return null;
    }

    @Nullable
    private static Integer getRemainingDays(@NotNull String normalized) {
        Matcher matcher = DAYS_REMAINING.matcher(normalized);
        if (matcher.find()) {
            String days = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
            try {
                return Integer.parseInt(days);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean isPurchased(@NotNull String normalized, boolean expiring) {
        return normalized.contains("single developer license") ||
            normalized.contains("multi-developer license") ||
            normalized.contains("site license") ||
            normalized.contains("server license") ||
            normalized.contains("purchased license") ||
            normalized.contains("production license") ||
            normalized.contains("subscription license") ||
            expiring && normalized.contains("license");
    }

    private static boolean isTrial(@NotNull String normalized) {
        if (!normalized.contains("trial license") && !normalized.contains("limited trial version")) {
            return false;
        }
        if (INVALID_WORD.matcher(normalized).find()) {
            return false;
        }
        return normalized.equals("trial license") || ACTIVE_WORD.matcher(normalized).find() ||
            VALID_WORD.matcher(normalized).find() || DAYS_REMAINING.matcher(normalized).find();
    }

    private static boolean hasUnavailableStatus(@NotNull String normalized) {
        return normalized.contains("not installed") || normalized.contains("unavailable") ||
            normalized.contains("unknown") || normalized.contains("inactive") ||
            normalized.contains("not active") || normalized.contains("not valid") ||
            normalized.contains("failed") || normalized.contains("failure") ||
            normalized.contains("revoked") || normalized.contains("disabled") ||
            normalized.contains("suspended") || normalized.contains("denied") ||
            normalized.contains("unlicensed");
    }

    @NotNull
    private static String getValue(@NotNull Map<?, ?> information, @NotNull String key) {
        Object value = information.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
