/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;

public class VirtualUniqueKeyEditAction extends Action {
    private ResultSetViewer resultSetViewer;
    private boolean define;

    public VirtualUniqueKeyEditAction(ResultSetViewer resultSetViewer, boolean define)
    {
        super(define ? "Define virtual unique key" : "Clear virtual unique key");
        this.resultSetViewer = resultSetViewer;
        this.define = define;
    }

    @Override
    public boolean isEnabled()
    {
        DBVEntity vEntity = resultSetViewer.getModel().getVirtualEntity(false);
        DBVEntityConstraint vConstraint = vEntity == null ? null : vEntity.getBestIdentifier();

        return vConstraint != null && (define != vConstraint.hasAttributes());
    }

    @Override
    public void run()
    {
        if (define) {
            resultSetViewer.editEntityIdentifier();
        } else {
            resultSetViewer.clearEntityIdentifier();
        }
    }
}
