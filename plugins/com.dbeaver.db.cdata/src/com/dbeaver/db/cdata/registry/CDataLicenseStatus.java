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

public enum CDataLicenseStatus {
    NOT_INSTALLED,
    TRIAL_ACTIVE,
    TRIAL_EXPIRING,
    TRIAL_EXPIRED,
    PURCHASED_ACTIVE,
    PURCHASED_EXPIRING,
    EXPIRED,
    INVALID_KEY,
    MACHINE_MISMATCH,
    WRONG_MAJOR_VERSION,
    VALIDATION_UNAVAILABLE;

    public boolean isValid() {
        return this == TRIAL_ACTIVE || this == TRIAL_EXPIRING ||
            this == PURCHASED_ACTIVE || this == PURCHASED_EXPIRING;
    }

    public boolean isTrial() {
        return this == TRIAL_ACTIVE || this == TRIAL_EXPIRING || this == TRIAL_EXPIRED;
    }
}
