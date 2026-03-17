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
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.GroupingResultsContainer;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.action.ExtensionPointAction;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.action.GroupingAction;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.column.impl.TransformerGroupingFunctionColumn;

public class GroupingActionDescriptor extends AbstractDescriptor {

    public static final String TAG_ACTION = "action"; //$NON-NLS-1$

    @NotNull
    private final String preferenceKey;

    @Nullable
    private final String label;

    @Nullable
    private final String description;

    @Nullable
    private final DBPImage icon;

    @NotNull
    private final ObjectType columnObjectType;

    public GroupingActionDescriptor(@NotNull IConfigurationElement config) {
        super(config);
        this.columnObjectType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        this.preferenceKey = config.getAttribute(RegistryConstants.ATTR_PREFERENCE_KEY);
    }

    @NotNull
    public TransformerGroupingFunctionColumn createColumn(
        @NotNull DBPDataSource dataSource,
        @NotNull GroupingResultsContainer groupingResultsContainer
    ) throws DBException {
        return columnObjectType.createInstance(
            TransformerGroupingFunctionColumn.class,
            dataSource,
            groupingResultsContainer,
            preferenceKey
        );
    }

    @NotNull
    public GroupingAction createAction(@NotNull GroupingResultsContainer groupingResultsContainer) {
        return new ExtensionPointAction(groupingResultsContainer, this);
    }

    @Nullable
    public String getLabel() {
        return label;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public DBPImage getIcon() {
        return icon;
    }

    @NotNull
    public String getPreferenceKey() {
        return preferenceKey;
    }
}
