/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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

    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof IDatabaseEditor)) {
            return false;
        }
        IDatabaseEditor databaseEditor = (IDatabaseEditor) receiver;
        if (property.equals(PROP_ACTIVE)) {
            String typeName = String.valueOf(expectedValue);
            if (databaseEditor instanceof MultiPageDatabaseEditor<?>) {
                MultiPageDatabaseEditor<?> mpEditor = (MultiPageDatabaseEditor<?>)databaseEditor;
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