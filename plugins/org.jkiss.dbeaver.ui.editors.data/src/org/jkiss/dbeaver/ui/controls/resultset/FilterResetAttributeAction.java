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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.action.Action;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;

class FilterResetAttributeAction extends Action {
    private ResultSetViewer resultSetViewer;
    private final DBDAttributeBinding attribute;
    FilterResetAttributeAction(ResultSetViewer resultSetViewer, DBDAttributeBinding attribute)
    {
        super("Remove filter for '" + attribute.getName() + "'", DBeaverIcons.getImageDescriptor(UIIcon.REVERT));
        this.resultSetViewer = resultSetViewer;
        this.attribute = attribute;
    }

    @Override
    public void run()
    {
        DBDDataFilter dataFilter = new DBDDataFilter(resultSetViewer.getModel().getDataFilter());
        DBDAttributeConstraint constraint = dataFilter.getConstraint(attribute);
        if (constraint != null) {
            constraint.setCriteria(null);
            resultSetViewer.setDataFilter(dataFilter, true);
        }
    }
}
