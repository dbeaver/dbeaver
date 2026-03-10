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
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.descriptor;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.GroupingResultsContainer;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.column.TransformerGroupingFunctionColumn;

public class TransformerGroupingFunctionColumnDescriptor extends AbstractDescriptor {

    public static final String TAG_COLUMN = "column"; //$NON-NLS-1$

    private final ObjectType column;

    public TransformerGroupingFunctionColumnDescriptor(@NotNull IConfigurationElement contributorConfig) {
        super(contributorConfig);
        this.column = new ObjectType(contributorConfig.getAttribute(RegistryConstants.ATTR_CLASS));
    }

    @NotNull
    public ObjectType getColumn() {
        return column;
    }

    @NotNull
    public TransformerGroupingFunctionColumn getColumn(
        @NotNull DBPDataSource dataSource,
        @NotNull GroupingResultsContainer groupingResultsContainer
    )
    throws DBException {
        return column.createInstance(TransformerGroupingFunctionColumn.class, dataSource, groupingResultsContainer);
    }
}
