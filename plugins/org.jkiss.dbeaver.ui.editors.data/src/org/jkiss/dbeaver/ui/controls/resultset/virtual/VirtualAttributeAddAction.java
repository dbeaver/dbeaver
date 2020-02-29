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
package org.jkiss.dbeaver.ui.controls.resultset.virtual;

import org.eclipse.jface.action.Action;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVEntityAttribute;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;

public class VirtualAttributeAddAction extends Action {

    private ResultSetViewer resultSetViewer;

    public VirtualAttributeAddAction(ResultSetViewer resultSetViewer) {
        super("Add virtual column");
        this.resultSetViewer = resultSetViewer;
    }

    @Override
    public void run()
    {
        DBVEntity vEntity = resultSetViewer.getModel().getVirtualEntity(false);
        DBVEntityAttribute vAttr = new DBVEntityAttribute(vEntity, null, "vcolumn");
        if (new EditVirtualAttributePage(resultSetViewer, vAttr).edit(resultSetViewer.getControl().getShell())) {
            vAttr.setCustom(true);
            vEntity.addVirtualAttribute(vAttr);
            vEntity.persistConfiguration();
            resultSetViewer.refreshMetaData();
            DBDAttributeConstraint vAttrConstr = resultSetViewer.getModel().getDataFilter().getConstraint(vAttr, false);
            if (vAttrConstr != null) {

            }
        }
    }
}
