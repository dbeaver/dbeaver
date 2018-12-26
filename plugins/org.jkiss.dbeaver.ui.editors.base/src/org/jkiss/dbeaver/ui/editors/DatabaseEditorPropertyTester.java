/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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