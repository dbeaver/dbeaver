/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ext.IDatabaseEditor;

/**
 * DatabaseEditorPropertyTester
 */
public class DatabaseEditorPropertyTester extends PropertyTester
{
    public static final String PROP_ACTIVE = "active";

    public DatabaseEditorPropertyTester() {
        super();
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof IDatabaseEditor)) {
            return false;
        }
        IDatabaseEditor databaseEditor = (IDatabaseEditor) receiver;
        if (property.equals(PROP_ACTIVE)) {
            String typeName = String.valueOf(expectedValue);
            if (databaseEditor instanceof MultiPageAbstractEditor) {
                MultiPageAbstractEditor mpEditor = (MultiPageAbstractEditor)databaseEditor;
                IEditorPart activeEditor = mpEditor.getActiveEditor();
                return activeEditor != null && testObjectClass(activeEditor, typeName);
            } else {
                return testObjectClass(databaseEditor, typeName);
            }
        }
        return false;
    }

    private static boolean testObjectClass(Object object, String className)
    {
        for (Class clazz = object.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            if (clazz.getName().equals(className)) {
                return true;
            }
        }
        return false;
    }

}