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
package org.jkiss.dbeaver.ui.editors.sql.generator;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.tools.ToolsRegistry;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetSelection;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.CommonUtils;

/**
 * SQLUtilsPropertyTester
 */
public class SQLUtilsPropertyTester extends PropertyTester
{
    public static final String NAMESPACE = "org.jkiss.dbeaver.ui.editors.sql.util";
    public static final String PROP_CAN_GENERATE = "canGenerate";
    public static final String PROP_HAS_TOOLS = "hasTools";

    public SQLUtilsPropertyTester() {
        super();
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof IWorkbenchPart)) {
            return false;
        }
        IStructuredSelection structuredSelection = GenerateSQLContributor.getSelectionFromPart((IWorkbenchPart)receiver);
        if (structuredSelection == null || structuredSelection.isEmpty()) {
            return false;
        }
        switch (property) {
            case PROP_CAN_GENERATE:
                if (structuredSelection instanceof IResultSetSelection) {
                    // Results
                    return ((IResultSetSelection) structuredSelection).getController().getModel().isSingleSource();
                } else {
                    return GenerateSQLContributor.hasContributions(structuredSelection);
                }
            case PROP_HAS_TOOLS: {
                DBSObject object = NavigatorUtils.getSelectedObject(structuredSelection);
                return object != null && !CommonUtils.isEmpty(ToolsRegistry.getInstance().getTools(structuredSelection));
            }
        }
        return false;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

}