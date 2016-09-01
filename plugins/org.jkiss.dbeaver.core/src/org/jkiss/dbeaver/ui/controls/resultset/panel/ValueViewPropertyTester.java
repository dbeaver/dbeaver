/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.controls.resultset.panel;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPanel;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetCommandHandler;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.utils.CommonUtils;

/**
 * ValueViewPropertyTester
 */
public class ValueViewPropertyTester extends PropertyTester
{
    public static final String NAMESPACE = "org.jkiss.dbeaver.core.resultset.panel.valueView";

    public static final String PROP_ACTIVE = "active";

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        ResultSetViewer rsv = (ResultSetViewer) ResultSetCommandHandler.getActiveResultSet((IWorkbenchPart) receiver);
        return rsv != null && checkResultSetProperty(rsv, property, expectedValue);
    }

    private boolean checkResultSetProperty(ResultSetViewer rsv, String property, Object expectedValue)
    {
        IResultSetPanel visiblePanel = rsv.getVisiblePanel();
        if (visiblePanel instanceof ViewValuePanel) {
            switch (property) {
                case PROP_ACTIVE:
                    return true;
            }
        }
        return false;
    }

}