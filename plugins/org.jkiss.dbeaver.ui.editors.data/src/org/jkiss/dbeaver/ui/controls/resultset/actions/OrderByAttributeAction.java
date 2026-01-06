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

import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;

public class OrderByAttributeAction extends AbstractResultSetViewerAction {
    private final DBDAttributeBinding attribute;
    private final IResultSetController.ColumnOrder order;

    public OrderByAttributeAction(ResultSetViewer resultSetViewer, DBDAttributeBinding attribute, IResultSetController.ColumnOrder order) {
        super(
            resultSetViewer,
            order == IResultSetController.ColumnOrder.NONE ?
                "Disable order by " + attribute.getName() :
                "Order by " + attribute.getName() + " " + order.name(), AS_CHECK_BOX);
        this.attribute = attribute;
        this.order = order;
        if (order != IResultSetController.ColumnOrder.NONE) {
            setImageDescriptor(DBeaverIcons.getImageDescriptor(order != IResultSetController.ColumnOrder.ASC ?
                UIIcon.SORT_INCREASE : UIIcon.SORT_DECREASE));
        }
    }

    @Override
    public boolean isChecked() {
        if (order == IResultSetController.ColumnOrder.NONE) {
            return false;
        }
        DBDDataFilter dataFilter = getResultSetViewer().getModel().getDataFilter();
        DBDAttributeConstraint constraint = dataFilter.getConstraint(attribute);
        if (constraint == null || constraint.getOrderPosition() <= 0) {
            return false;
        }
        boolean forceAsc = order == IResultSetController.ColumnOrder.ASC;
        return constraint.isOrderDescending() != forceAsc;
    }

    @Override
    public void run() {
        getResultSetViewer().toggleSortOrder(attribute, order);
    }
}
