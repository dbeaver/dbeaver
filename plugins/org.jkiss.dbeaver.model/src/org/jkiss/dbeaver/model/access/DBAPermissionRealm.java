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

package org.jkiss.dbeaver.model.access;

/**
 * Permission realm
 */
public interface DBAPermissionRealm {

    /**
     * Access to all public API for authorized users
     */
    String PERMISSION_PUBLIC = "public";
    /**
     * Access to private resources (e.g. private project)
     */
    String PERMISSION_PRIVATE_OWNER = "private";
    /**
     * Admin access to all API
     */
    String PERMISSION_ADMIN = "admin";

    boolean hasRealmPermission(String permission);

    boolean supportsRealmFeature(String feature);

}
