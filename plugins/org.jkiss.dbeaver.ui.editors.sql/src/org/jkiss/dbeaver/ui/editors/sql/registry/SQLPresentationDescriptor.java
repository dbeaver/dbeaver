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

package org.jkiss.dbeaver.ui.editors.sql.registry;

import org.eclipse.core.expressions.Expression;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorPresentation;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLPresentationDescriptor
 */
public class SQLPresentationDescriptor extends AbstractContextDescriptor {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.sqlPresentation"; //$NON-NLS-1$

    public enum QueryMode {
        /** The presentation can work with a single statement */
        SINGLE,
        /** The presentation can work with multiple statements (script) */
        MULTIPLE,
        /** The presentation doesn't support queries */
        NONE
    }

    private final String id;
    private final String label;
    private final String description;
    private final ObjectType implClass;
    private final DBPImage icon;
    private final int order;
    private final List<SQLPresentationPanelDescriptor> panels = new ArrayList<>();
    private final Expression enabledWhen;
    private final QueryMode queryMode;

    public SQLPresentationDescriptor(IConfigurationElement config)
    {
        super(config);
        this.id = config.getAttribute("id");
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.implClass = new ObjectType(config.getAttribute("class"));
        this.icon = iconToImage(config.getAttribute("icon"));
        this.order = CommonUtils.toInt(config.getAttribute(RegistryConstants.ATTR_ORDER));
        for (IConfigurationElement panelConfig : config.getChildren("panel")) {
            this.panels.add(new SQLPresentationPanelDescriptor(panelConfig));
        }
        this.enabledWhen = getEnablementExpression(config);
        this.queryMode = CommonUtils.valueOf(QueryMode.class, config.getAttribute("queryMode"), QueryMode.MULTIPLE);
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

    public int getOrder() {
        return order;
    }

    public List<SQLPresentationPanelDescriptor> getPanels() {
        return panels;
    }

    @Nullable
    public Expression getEnabledWhen() {
        return enabledWhen;
    }

    public boolean isEnabled(@NotNull IWorkbenchSite site) {
        return isExpressionTrue(enabledWhen, site);
    }

    @NotNull
    public QueryMode getQueryMode() {
        return queryMode;
    }

    public SQLEditorPresentation createPresentation()
        throws DBException
    {
        return implClass.createInstance(SQLEditorPresentation.class);
    }

}
