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
package org.jkiss.dbeaver.model.datadam.rpc;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.HashSet;
import java.util.List;

public class DDDesktopRpcRegistrationDescriptor extends AbstractDescriptor {
    private final String id;
    private final List<String> dependsOn;
    private final List<String> registerAfter;
    private final ObjectType registrationType;

    public DDDesktopRpcRegistrationDescriptor(@NotNull IConfigurationElement extension) {
        super(extension);
        id = extension.getAttribute("id");
        if (CommonUtils.isEmpty(id) || id.isBlank()) {
            throw new IllegalArgumentException("Desktop RPC registration ID is missing");
        }
        dependsOn = parseIds(extension.getAttribute("dependsOn"));
        registerAfter = parseIds(extension.getAttribute("registerAfter"));
        registrationType = new ObjectType(extension, "class");
    }

    @NotNull
    private List<String> parseIds(@Nullable String value) {
        List<String> ids = CommonUtils.splitString(value, ',').stream().map(String::trim).toList();
        if (ids.stream().anyMatch(String::isEmpty) || new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("Invalid desktop RPC registration dependencies: " + value);
        }
        return ids;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public List<String> getDependsOn() {
        return dependsOn;
    }

    @NotNull
    public List<String> getRegisterAfter() {
        return registerAfter;
    }

    @NotNull
    public DDDesktopRpcRegistration create(@NotNull List<DDDesktopRpcRegistration> dependencies) throws DBException {
        return registrationType.createInstance(DDDesktopRpcRegistration.class, dependencies.toArray());
    }
}
