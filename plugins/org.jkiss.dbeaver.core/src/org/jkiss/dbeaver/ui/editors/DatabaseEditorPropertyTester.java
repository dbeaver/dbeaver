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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.IEditorPart;

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