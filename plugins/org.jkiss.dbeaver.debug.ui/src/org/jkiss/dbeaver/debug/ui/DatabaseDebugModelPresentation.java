/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.core.DebugUtils;
import org.jkiss.dbeaver.debug.core.breakpoints.DatabaseLineBreakpoint;
import org.jkiss.dbeaver.debug.core.breakpoints.IDatabaseBreakpoint;
import org.jkiss.dbeaver.debug.core.model.*;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;

import java.util.HashMap;
import java.util.Map;

public class DatabaseDebugModelPresentation extends LabelProvider implements IDebugModelPresentationExtension {

    private static Log log = Log.getLog(DatabaseDebugModelPresentation.class);

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
                int lineNumber = breakpoint.getLineNumber();
                Object[] bindings = new Object[] { breakpoint.getObjectName(), lineNumber };
                return NLS.bind("{0} - [line:{1}]", bindings);
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
            IStatus status = DebugUtils.newErrorStatus(message, e);
            log.log(status);
            listener.detailComputed(value, e.getMessage());
        }
    }

    @Override
    public IEditorInput getEditorInput(Object element) {
        if (element instanceof DBNDatabaseNode) {
            DBNDatabaseNode databaseNode = (DBNDatabaseNode) element;
            return createEditorInput(databaseNode);
        }
        if (element instanceof IDatabaseBreakpoint) {
            IDatabaseBreakpoint breakpoint = (IDatabaseBreakpoint) element;
            try {
                String nodePath = breakpoint.getNodePath();
                DBNModel navigatorModel = DBWorkbench.getPlatform().getNavigatorModel();
                DBNNode node = navigatorModel.getNodeByPath(new VoidProgressMonitor(), nodePath);
                if (node instanceof DBNDatabaseNode) {
                    DBNDatabaseNode databaseNode = (DBNDatabaseNode) node;
                    return createEditorInput(databaseNode);
                }
            } catch (Exception e) {
                String message = NLS.bind("Unable to resolve editor input for breakpoint {0}", breakpoint);
                log.error(message, e);
            }
            
        }
        
        return null;
    }

    protected IEditorInput createEditorInput(DBNDatabaseNode dbnNode) {
        DBGEditorAdvisor editorAdvisor = DebugUI.findEditorAdvisor(dbnNode.getDataSourceContainer());
        String sourceFolderId = editorAdvisor == null ? null : editorAdvisor.getSourceFolderId();
        Map<String, Object> editorAttrs = new HashMap<>();
        editorAttrs.put(DBPScriptObject.OPTION_DEBUGGER_SOURCE, true);

        IEditorPart editorPart = new UITask<IEditorPart>() {
            @Override
            protected IEditorPart runTask() {
                return NavigatorHandlerObjectOpen.openEntityEditor(dbnNode, null, sourceFolderId, editorAttrs, UIUtils.getActiveWorkbenchWindow(), false);
            }
        }.execute();

        return editorPart == null ? null : editorPart.getEditorInput();
    }

    @Override
    public String getEditorId(IEditorInput input, Object element) {
        return EntityEditor.ID;
    }

    @Override
    public boolean requiresUIThread(Object element) {
        return false;
    }

}
