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
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;

public class VirtualForeignKeyEditAction extends Action {

    private ResultSetViewer resultSetViewer;

    public VirtualForeignKeyEditAction(ResultSetViewer resultSetViewer) {
        super("Add virtual foreign key");
        this.resultSetViewer = resultSetViewer;
    }

    @Override
    public void run()
    {
        if (EditForeignKeyPage.createVirtualForeignKey(resultSetViewer.getModel().getVirtualEntity(true)) != null) {
            resultSetViewer.persistConfig();
            resultSetViewer.refreshMetaData();
        }
    }
}
