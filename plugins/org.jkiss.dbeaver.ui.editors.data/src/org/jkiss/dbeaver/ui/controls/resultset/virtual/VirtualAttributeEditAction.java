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
package org.jkiss.dbeaver.ui.controls.resultset.virtual;

import org.eclipse.jface.action.Action;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeBindingCustom;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVEntityAttribute;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;

public class VirtualAttributeEditAction extends Action {
    private ResultSetViewer resultSetViewer;
    private DBDAttributeBinding attr;
    public VirtualAttributeEditAction(ResultSetViewer resultSetViewer, DBDAttributeBinding attr) {
        super("Edit virtual column '" + attr.getName() + "'");
        this.resultSetViewer = resultSetViewer;
        this.attr = attr;
    }

    @Override
    public boolean isEnabled() {
        return (attr instanceof DBDAttributeBindingCustom);
    }

    @Override
    public void run() {
        if (attr == null) {
            return;
        }
        DBVEntityAttribute vAttr = ((DBDAttributeBindingCustom)attr).getEntityAttribute();
        DBVEntity vEntity = resultSetViewer.getModel().getVirtualEntity(false);
        if (new EditVirtualAttributePage(resultSetViewer, vAttr).edit(resultSetViewer.getControl().getShell())) {
            vEntity.persistConfiguration();
            resultSetViewer.refreshMetaData();
        }
    }
}
