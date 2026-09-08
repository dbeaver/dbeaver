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

    /**
     * The license state could not be determined: the validation probe did not run or CData
     * returned an answer we do not recognize. This is not a statement about the license itself.
     */
    public boolean isUnknown() {
        return this == VALIDATION_UNAVAILABLE;
    }

    /**
     * The driver may be used: either the license is valid, or its state is unknown.
     * In the latter case CData itself reports the problem when a connection is opened,
     * so we must not block a user whose license we simply failed to read.
     */
    public boolean allowsDriverUsage() {
        return isValid() || isUnknown();
    }
}
