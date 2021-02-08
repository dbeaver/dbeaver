/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorPresentation;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SQLPresentationDescriptor
 */
public class SQLPresentationDescriptor extends AbstractContextDescriptor {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.sqlPresentation"; //$NON-NLS-1$

    private final String id;
    private final String label;
    private final String description;
    private final ObjectType implClass;
    private final DBPImage icon;
    private final SQLEditorPresentation.ActivationType activationType;
    private final String toggleCommandId;
    private final List<SQLPresentationPanelDescriptor> panels = new ArrayList<>();

    public SQLPresentationDescriptor(IConfigurationElement config)
    {
        super(config);
        this.id = config.getAttribute("id");
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.implClass = new ObjectType(config.getAttribute("class"));
        this.icon = iconToImage(config.getAttribute("icon"));
        String activationStr = config.getAttribute("activation");
        if (CommonUtils.isEmpty(activationStr)) {
            this.activationType = SQLEditorPresentation.ActivationType.HIDDEN;
        } else {
            this.activationType = SQLEditorPresentation.ActivationType.valueOf(activationStr.toUpperCase(Locale.ENGLISH));
        }
        toggleCommandId = config.getAttribute("toggleCommand");
        for (IConfigurationElement panelConfig : config.getChildren("panel")) {
            // Load functions
            SQLPresentationPanelDescriptor presentationDescriptor = new SQLPresentationPanelDescriptor(panelConfig);
            this.panels.add(presentationDescriptor);
        }
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

    public SQLEditorPresentation.ActivationType getActivationType() {
        return activationType;
    }

    public String getToggleCommandId() {
        return toggleCommandId;
    }

    public List<SQLPresentationPanelDescriptor> getPanels() {
        return panels;
    }

    public SQLEditorPresentation createPresentation()
        throws DBException
    {
        return implClass.createInstance(SQLEditorPresentation.class);
    }

}
