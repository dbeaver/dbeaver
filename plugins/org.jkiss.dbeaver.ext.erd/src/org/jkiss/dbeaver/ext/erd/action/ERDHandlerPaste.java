/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.erd.action;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ext.erd.command.EntityAddCommand;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorAdapter;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorPart;
import org.jkiss.dbeaver.ext.erd.model.DiagramObjectCollector;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.ui.dnd.DatabaseObjectTransfer;
import org.jkiss.utils.CommonUtils;

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
                    final List<ERDEntity> erdEntities = DiagramObjectCollector.generateEntityList(editor.getDiagram(), objects, true);
                    if (!CommonUtils.isEmpty(erdEntities)) {
                        Command command = editor.getDiagramPart().createEntityAddCommand(erdEntities, new Point(10, 10));
                        editor.getCommandStack().execute(command);
                    }
                }
            }
        }
        return null;
    }

}
