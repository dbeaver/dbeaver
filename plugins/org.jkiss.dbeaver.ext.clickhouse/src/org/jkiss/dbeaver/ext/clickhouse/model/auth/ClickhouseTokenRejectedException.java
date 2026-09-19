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
package org.jkiss.dbeaver.ext.clickhouse.model.auth;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;

/**
 * Thrown when the identity provider token is rejected (expired or revoked) and the user has to sign in again.
 */
public class ClickhouseTokenRejectedException extends DBException {
    public ClickhouseTokenRejectedException(@NotNull String message) {
        super(message);
    }
}
