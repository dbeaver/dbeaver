/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPPage;

public class SMUserFilter {
    @Nullable
    private String userIdMask;
    @Nullable
    private Boolean enabledState;

    @NotNull
    private DBPPage page;

    public SMUserFilter(@NotNull DBPPage page) {
        this.userIdMask = null;
        this.enabledState = null;
        this.page = page;
    }

    public SMUserFilter(@Nullable String userIdMask, @Nullable Boolean enabledState, @NotNull DBPPage page) {
        this.userIdMask = userIdMask;
        this.enabledState = enabledState;
        this.page = page;
    }

    @Nullable
    public String getUserIdMask() {
        return userIdMask;
    }

    @Nullable
    public Boolean getEnabledState() {
        return enabledState;
    }

    @NotNull
    public DBPPage getPage() {
        return page;
    }

    public void setPage(@NotNull DBPPage page) {
        this.page = page;
    }

    public void setUserIdMask(@Nullable String userIdMask) {
        this.userIdMask = userIdMask;
    }

    public void setEnabledState(@Nullable Boolean enabledState) {
        this.enabledState = enabledState;
    }

}
