/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;

import java.util.Iterator;

/**
 * DatabaseEditorPropertyTester
 */
public class ERDEditorPropertyTester extends PropertyTester
{
    public static final String NAMESPACE = "org.jkiss.dbeaver.ext.erd.editor";
    public static final String PROP_CAN_UNDO = "canUndo";
    public static final String PROP_CAN_REDO = "canRedo";
    public static final String PROP_CAN_DELETE = "canDelete";
    public static final String PROP_EDITABLE = "editable";

    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof ERDEditorPart)) {
            return false;
        }
        ERDEditorPart erdEditor = (ERDEditorPart) receiver;
        if (property.equals(PROP_CAN_UNDO)) {
            return erdEditor.getCommandStack().canUndo();
        } else if (property.equals(PROP_CAN_REDO)) {
            return erdEditor.getCommandStack().canRedo();
        } else if (property.equals(PROP_EDITABLE)) {
            return !erdEditor.isReadOnly();
        } else if (property.equals(PROP_CAN_DELETE)) {
            DeleteAction deleteAction = new DeleteAction((IWorkbenchPart) erdEditor);
            deleteAction.update();
            return deleteAction.isEnabled();
        }
        return false;
    }

}