/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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

package org.jkiss.dbeaver.debug.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDebugModelPresentationExtension;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.jkiss.dbeaver.debug.core.DebugCore;
import org.jkiss.dbeaver.debug.core.breakpoints.DatabaseLineBreakpoint;
import org.jkiss.dbeaver.debug.core.model.DatabaseProcess;
import org.jkiss.dbeaver.debug.core.model.DatabaseStackFrame;
import org.jkiss.dbeaver.debug.core.model.DatabaseThread;
import org.jkiss.dbeaver.debug.core.model.DatabaseVariable;
import org.jkiss.dbeaver.debug.core.model.IDatabaseDebugTarget;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorInput;

public class DatabaseDebugModelPresentation extends LabelProvider implements IDebugModelPresentationExtension {

    private final Map<String, Object> attributes = new HashMap<>();

    private final ILabelProvider labelProvider;

    public DatabaseDebugModelPresentation() {
        this(WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
    }

    public DatabaseDebugModelPresentation(ILabelProvider labelProvider) {
        this.labelProvider = labelProvider;
    }

    public Object getAttribute(String attribute) {
        return attributes.get(attribute);
    }

    @Override
    public void setAttribute(String attribute, Object value) {
        attributes.put(attribute, value);
    }

    @Override
    public Image getImage(Object element) {
        return labelProvider.getImage(element);
    }

    @Override
    public String getText(Object element) {
        // FIXME:AF: register adapters
        try {
            if (element instanceof IDatabaseDebugTarget) {
                IDatabaseDebugTarget databaseDebugTarget = (IDatabaseDebugTarget) element;
                return databaseDebugTarget.getName();
            }
            if (element instanceof DatabaseProcess) {
                DatabaseProcess process = (DatabaseProcess) element;
                return process.getLabel();
            }
            if (element instanceof DatabaseThread) {
                DatabaseThread thread = (DatabaseThread) element;
                return thread.getName();
            }
            if (element instanceof DatabaseStackFrame) {
                DatabaseStackFrame stackFrame = (DatabaseStackFrame) element;
                return stackFrame.getName();
            }
            if (element instanceof DatabaseVariable) {
                DatabaseVariable variable = (DatabaseVariable) element;
                return variable.getName();
            }
            if (element instanceof DatabaseLineBreakpoint) {
                DatabaseLineBreakpoint breakpoint = (DatabaseLineBreakpoint) element;
                String database = breakpoint.getDatabaseName();
                String schema = breakpoint.getSchemaName();
                String procedure = breakpoint.getProcedureName();
                int lineNumber = breakpoint.getLineNumber();
                String pattern = "{0}.{1}.{2} - [line:{3}]";
                Object[] bindings = new Object[] {database, schema, procedure, lineNumber};
                return NLS.bind(pattern, bindings);
            }
        } catch (CoreException e) {
            return "<not responding>";
        }
        return labelProvider.getText(element);
    }

    @Override
    public void dispose() {
        attributes.clear();
        super.dispose();
    }

    @Override
    public void computeDetail(IValue value, IValueDetailListener listener) {
        try {
            String valueString = value.getValueString();
            listener.detailComputed(value, valueString);
        } catch (DebugException e) {
            String message = NLS.bind("Unable to compute valie for {0}", value);
            IStatus status = DebugCore.newErrorStatus(message, e);
            DebugCore.log(status);
            listener.detailComputed(value, e.getMessage());
        }
    }

    @Override
    public IEditorInput getEditorInput(Object element) {
        if (element instanceof DBNDatabaseNode) {
            DBNDatabaseNode dbnNode = (DBNDatabaseNode) element;
            EntityEditorInput editorInput = new EntityEditorInput(dbnNode);
            editorInput.setAttribute(DBPScriptObject.OPTION_DEBUGGER_SOURCE, Boolean.TRUE);
// FIXME:AF: how to retrieve it? probably org.jkiss.dbeaver.databaseor and EntityEditorsRegistry can help
            // String folderId = "postgresql.source.view";
            // editorInput.setDefaultFolderId(folderId);
            return editorInput;
        }
        return null;
    }

    @Override
    public String getEditorId(IEditorInput input, Object element) {
        // FIXME:AF: is there a constant anywhere?
        return "org.jkiss.dbeaver.ui.editors.entity.EntityEditor";
    }

    @Override
    public boolean requiresUIThread(Object element) {
        return false;
    }

}
