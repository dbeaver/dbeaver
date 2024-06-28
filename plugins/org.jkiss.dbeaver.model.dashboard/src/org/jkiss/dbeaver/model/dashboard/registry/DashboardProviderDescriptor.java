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
package org.jkiss.dbeaver.model.dashboard.registry;

import org.eclipse.core.expressions.Expression;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.dashboard.DBDashboardDataType;
import org.jkiss.dbeaver.model.dashboard.DBDashboardProvider;
import org.jkiss.dbeaver.model.dashboard.DashboardConstants;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.utils.CommonUtils;

/**
 * DashboardProviderDescriptor
 */
public class DashboardProviderDescriptor extends AbstractContextDescriptor implements DBPNamedObject {
    private static final Log log = Log.getLog(DashboardProviderDescriptor.class);

    private final String id;
    private final String label;
    private final String description;
    private final DBPImage icon;
    private final ObjectType implType;
    private final boolean supportsCustomDashboards;
    private final boolean supportsFolders;
    private final boolean databaseRequired;
    private final String defaultRenderer;
    private final DBDashboardDataType dataType;
    private DBDashboardProvider instance;
    private final Expression enabledWhen;

    public DashboardProviderDescriptor(IConfigurationElement config) {
        super(config);
        this.id = config.getAttribute("id");
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.icon = iconToImage(config.getAttribute("icon"));
        this.supportsCustomDashboards = CommonUtils.toBoolean(config.getAttribute("supportsCustomization"));
        this.supportsFolders = CommonUtils.toBoolean(config.getAttribute("supportsFolders"));
        this.databaseRequired = CommonUtils.toBoolean(config.getAttribute("databaseRequired"));
        this.defaultRenderer = CommonUtils.toString(config.getAttribute("defaultRenderer"));
        this.dataType = CommonUtils.valueOf(
            DBDashboardDataType.class,
            config.getAttribute("dataType"),
            DashboardConstants.DEF_DASHBOARD_DATA_TYPE
        );
        this.implType = new ObjectType(config.getAttribute("class"));
        this.enabledWhen = getEnablementExpression(config);
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public DBPImage getIcon() {
        return icon;
    }

    public ObjectType getImplType() {
        return implType;
    }

    public boolean isSupportsCustomDashboards() {
        return supportsCustomDashboards;
    }

    public boolean isSupportsFolders() {
        return supportsFolders;
    }

    public boolean isDatabaseRequired() {
        return databaseRequired;
    }

    public String getDefaultRenderer() {
        return defaultRenderer;
    }

    @NotNull
    public DBDashboardDataType getDataType() {
        return dataType;
    }

    @Override
    public boolean appliesTo(DBPObject object, Object context) {
        return object instanceof DBPDataSourceContainer ds && getInstance().appliesTo(ds) && isExpressionTrue(enabledWhen, object);
    }

    public boolean isEnabled() {
        return isExpressionTrue(enabledWhen, this);
    }

    public DBDashboardProvider getInstance() {
        if (instance == null) {
            try {
                instance = implType.createInstance(DBDashboardProvider.class);
            } catch (Exception e) {
                throw new IllegalStateException("Cannot instantiate dashboard provider '" + id + "'", e);
            }
        }
        return instance;
    }

    @NotNull
    @Override
    public String getName() {
        return getLabel();
    }

    @Override
    public String toString() {
        return getName();
    }
}
