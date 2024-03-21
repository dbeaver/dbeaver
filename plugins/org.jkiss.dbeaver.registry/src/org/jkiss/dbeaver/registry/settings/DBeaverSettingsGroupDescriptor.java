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
package org.jkiss.dbeaver.registry.settings;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class DBeaverSettingsGroupDescriptor extends AbstractDescriptor {
    private final String id;
    private final String displayName;

    private DBeaverSettingsGroupDescriptor parentGroup;
    private final List<DBeaverSettingDescriptor> settings = new ArrayList<>();
    private final List<DBeaverSettingsGroupDescriptor> subGroups = new ArrayList<>();

    public DBeaverSettingsGroupDescriptor(IConfigurationElement cfg) {
        super(cfg);
        this.id = cfg.getAttribute("id");
        this.displayName = cfg.getAttribute("label");
    }

    synchronized void addSetting(@NotNull DBeaverSettingDescriptor setting) {
        this.settings.add(setting);
    }

    synchronized void addSubGroup(@NotNull DBeaverSettingsGroupDescriptor subGroup) {
        this.subGroups.add(subGroup);
    }

    @NotNull
    public List<DBeaverSettingDescriptor> getSettings() {
        return List.copyOf(settings);
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getFullId() {
        return (getParentGroup() != null ? (parentGroup.getFullId() + "/") : "") + getId();
    }

    @NotNull
    public String getDisplayName() {
        return CommonUtils.isEmpty(displayName) ? getId() : displayName;
    }

    @NotNull
    public List<DBeaverSettingsGroupDescriptor> getSubGroups() {
        return subGroups;
    }

    @Nullable
    public DBeaverSettingsGroupDescriptor getParentGroup() {
        return parentGroup;
    }

    public void setParentGroup(@NotNull DBeaverSettingsGroupDescriptor parentGroup) {
        this.parentGroup = parentGroup;
    }
}
