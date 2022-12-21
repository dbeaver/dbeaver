/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.auth;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.utils.CommonUtils;

/**
 * Auth provider property.
 * Has some extra attributes.
 */
public class AuthPropertyDescriptor extends PropertyDescriptor {

    private final AuthPropertyEncryption encryption;
    private final boolean identifying; // Identifying parameter. Will be used during auth for user search by credentials
    private final boolean admin; // Parameter value can be configured in admin panel
    private final boolean user; // Parameter can be passed by end-user from UI

    public AuthPropertyDescriptor(String category, IConfigurationElement config) {
        super(category, config);

        this.encryption = CommonUtils.valueOf(AuthPropertyEncryption.class, config.getAttribute("encryption"), AuthPropertyEncryption.none);
        this.identifying = CommonUtils.getBoolean(config.getAttribute("identifying"), false);
        this.admin = CommonUtils.getBoolean(config.getAttribute("admin"), false);
        this.user = CommonUtils.getBoolean(config.getAttribute("user"), false);
    }

    public AuthPropertyEncryption getEncryption() {
        return encryption;
    }

    public boolean isIdentifying() {
        return identifying;
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean isUser() {
        return user;
    }
}
