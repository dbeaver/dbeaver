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
package org.jkiss.dbeaver.ui.controls.resultset.actions;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.virtual.DBVTransformSettings;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;

public class TransformerAction extends AbstractResultSetViewerAction {
    private final DBDAttributeBinding attribute;

    public TransformerAction(ResultSetViewer resultSetViewer, DBDAttributeBinding attr, String text, int style, boolean checked) {
        super(resultSetViewer, text, style);
        this.attribute = attr;
        setChecked(checked);
    }

    @NotNull
    public DBVTransformSettings getTransformSettings() {
        final DBVTransformSettings settings = DBVUtils.getTransformSettings(attribute, true);
        if (settings == null) {
            throw new IllegalStateException("Can't get/create transformer settings for '" + attribute.getFullyQualifiedName(DBPEvaluationContext.UI) + "'");
        }
        return settings;
    }

    public void saveTransformerSettings() {
        attribute.getDataSource().getContainer().persistConfiguration();
        getResultSetViewer().refreshData(null);
    }
}
