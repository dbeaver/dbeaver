/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IEvaluationService;
import org.jkiss.dbeaver.model.edit.DBECommandContext;

/**
 * EntityEditorPropertyTester
 */
public class EntityEditorPropertyTester extends PropertyTester
{
    static final Log log = LogFactory.getLog(EntityEditorPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.ui.editors.entity";
    public static final String PROP_DIRTY = "dirty";
    public static final String PROP_CAN_UNDO = "canUndo";
    public static final String PROP_CAN_REDO = "canRedo";

    public EntityEditorPropertyTester() {
        super();
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof EntityEditor)) {
            return false;
        }
        EntityEditor editor = (EntityEditor)receiver;
        DBECommandContext commandContext = editor.getEditorInput().getCommandContext();
        if (property.equals(PROP_CAN_UNDO)) {
            return commandContext.getUndoCommand() != null;
        } else if (property.equals(PROP_CAN_REDO)) {
            return commandContext.getRedoCommand() != null;
        } else if (property.equals(PROP_DIRTY)) {
            return commandContext.isDirty();
        }

        return false;
    }

    public static void firePropertyChange(String propName)
    {
        IEvaluationService service = (IEvaluationService) PlatformUI.getWorkbench().getService(IEvaluationService.class);
        service.requestEvaluation(NAMESPACE + "." + propName);
    }

}