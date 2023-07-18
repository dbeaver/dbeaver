/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.security.user;

public class SMUserFilter {
    private final String userIdMask;
    private final Boolean enabledState;

    public SMUserFilter() {
        this.userIdMask = null;
        this.enabledState = null;
    }

    public SMUserFilter(String userIdMask, Boolean enabledState) {
        this.userIdMask = userIdMask;
        this.enabledState = enabledState;
    }

    public String getUserIdMask() {
        return userIdMask;
    }

    public Boolean getEnabledState() {
        return enabledState;
    }

    public static SMUserFilter Empty = new SMUserFilter();
}
