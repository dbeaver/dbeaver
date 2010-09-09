/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;
import org.jkiss.dbeaver.ui.editors.MultiPageDatabaseEditor;

/**
 * DatabaseEditorPropertyTester
 */
public class ERDEditorPropertyTester extends PropertyTester
{
    public static final String NAMESPACE = "org.jkiss.dbeaver.ext.erd.editor";
    public static final String PROP_CAN_UNDO = "canUndo";
    public static final String PROP_CAN_REDO = "canRedo";

    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof ERDEditor)) {
            return false;
        }
        ERDEditor erdEditor = (ERDEditor) receiver;
        if (property.equals(PROP_CAN_UNDO)) {
            return erdEditor.getCommandStack().canUndo();
        } else if (property.equals(PROP_CAN_REDO)) {
            return erdEditor.getCommandStack().canRedo();
        }
        return false;
    }

}