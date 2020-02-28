/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset.colors;

import org.eclipse.jface.action.Action;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;

abstract class ColorAction extends Action {
    private ResultSetViewer resultSetViewer;

    protected ColorAction(ResultSetViewer resultSetViewer, String name) {
        super(name);
        this.resultSetViewer = resultSetViewer;
    }
    @NotNull
    DBVEntity getColorsVirtualEntity()
        throws IllegalStateException
    {
        DBSDataContainer dataContainer = resultSetViewer.getDataContainer();
        if (dataContainer == null) {
            throw new IllegalStateException("No data container");
        }
        return DBVUtils.getVirtualEntity(dataContainer, true);
    }

    void updateColors(DBVEntity entity) {
        resultSetViewer.getModel().updateColorMapping(true);
        resultSetViewer.redrawData(false, false);
        entity.persistConfiguration();
    }
}
