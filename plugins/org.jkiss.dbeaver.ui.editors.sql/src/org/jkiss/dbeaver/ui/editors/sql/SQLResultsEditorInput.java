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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetContainer;
import org.jkiss.utils.CommonUtils;

public class SQLResultsEditorInput implements IEditorInput {
    private final IResultSetContainer container;

    public SQLResultsEditorInput(@NotNull IResultSetContainer container) {
        this.container = container;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return DBeaverIcons.getImageDescriptor(UIIcon.RS_GRID);
    }

    @Override
    public String getName() {
        final DBSDataContainer dataContainer = container.getDataContainer();

        if (dataContainer == null) {
            return "Data";
        } else {
            return CommonUtils.getSingleLineString(dataContainer.getName());
        }
    }

    @Override
    public String getToolTipText() {
        final DBSDataContainer dataContainer = container.getDataContainer();

        if (dataContainer == null) {
            return "Data";
        } else {
            return dataContainer.getDescription();
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isInstance(container)) {
            return adapter.cast(container);
        } else {
            return null;
        }
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @NotNull
    public IResultSetContainer getContainer() {
        return container;
    }
}
