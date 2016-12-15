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
package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.core.expressions.PropertyTester;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.ActionUtils;

/**
 * FolderEditorPropertyTester
 */
public class FolderEditorPropertyTester extends PropertyTester
{
    private static final Log log = Log.getLog(FolderEditorPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.ui.editors.folder";
    public static final String PROP_CAN_NAVIGATE = "canNavigate";

    public FolderEditorPropertyTester() {
        super();
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof FolderEditor)) {
            return false;
        }
        FolderEditor editor = (FolderEditor)receiver;
        switch (property) {
            case PROP_CAN_NAVIGATE:
                if (expectedValue instanceof Number && ((Number)expectedValue).intValue() == 1 || "1".equals(expectedValue)) {
                    return editor.getHistoryPosition() < editor.getHistorySize() - 1;
                } else {
                    return editor.getHistoryPosition() > 0;
                }
        }

        return false;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

}