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
package org.jkiss.dbeaver.model.datadam.auth.desktop;

import org.jkiss.code.NotNull;

/** Desktop bearer credential and the origins for which it was obtained. */
public record DDDesktopUserToken(
    @NotNull String accessToken,
    @NotNull String issuer,
    @NotNull String accountOrigin,
    @NotNull String storageOrigin
) {
    @Override
    public String toString() {
        return "DDDesktopUserToken[issuer=" + issuer + "]";
    }
}
