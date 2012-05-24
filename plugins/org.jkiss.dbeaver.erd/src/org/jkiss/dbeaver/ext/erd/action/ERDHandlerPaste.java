/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.action;

import org.jkiss.dbeaver.ext.erd.model.ERDEntity;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ext.erd.command.EntityAddCommand;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorAdapter;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorPart;
import org.jkiss.dbeaver.ext.erd.model.DiagramObjectCollector;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.ui.dnd.DatabaseObjectTransfer;

import java.util.Collection;
import java.util.List;

public class ERDHandlerPaste extends AbstractHandler {
    public ERDHandlerPaste() {

    }

    @Override
    public boolean isEnabled()
    {
        final Collection<DBPNamedObject> objects = DatabaseObjectTransfer.getFromClipboard();
        if (objects == null || CommonUtils.isEmpty(objects)) {
            return false;
        }
        for (DBPNamedObject object : objects) {
            if (object instanceof DBSTable || object instanceof DBSObjectContainer) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Control control = (Control) HandlerUtil.getVariable(event, ISources.ACTIVE_FOCUS_CONTROL_NAME);
        if (control != null) {
            ERDEditorPart editor = ERDEditorAdapter.getEditor(control);
            if (editor != null && !editor.isReadOnly()) {
                final Collection<DBPNamedObject> objects = DatabaseObjectTransfer.getInstance().getObject();
                if (!CommonUtils.isEmpty(objects)) {
                    final List<ERDEntity> erdEntities = DiagramObjectCollector.generateEntityList(editor.getDiagram(), objects);
                    if (!CommonUtils.isEmpty(erdEntities)) {
                        EntityAddCommand command = new EntityAddCommand(editor.getDiagramPart(), erdEntities, new Point(10, 10));
                        editor.getCommandStack().execute(command);
                    }
                }
            }
        }
        return null;
    }

}
