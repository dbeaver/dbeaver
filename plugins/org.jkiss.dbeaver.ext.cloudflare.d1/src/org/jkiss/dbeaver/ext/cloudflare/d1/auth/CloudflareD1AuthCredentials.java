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
package org.jkiss.dbeaver.ext.cloudflare.d1.auth;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNativeCredentials;
import org.jkiss.dbeaver.model.meta.Property;

/** Native credentials represented by Cloudflare's account ID and API token. */
public class CloudflareD1AuthCredentials extends AuthModelDatabaseNativeCredentials {
    @Override
    @Nullable
    @Property(order = 1)
    public String getUserName() {
        return super.getUserName();
    }

    @Override
    @Nullable
    @Property(order = 2, password = true)
    public String getUserPassword() {
        return super.getUserPassword();
    }
}
