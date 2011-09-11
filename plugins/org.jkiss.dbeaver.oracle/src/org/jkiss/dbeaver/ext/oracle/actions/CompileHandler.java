/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.oracle.model.OracleCompileError;
import org.jkiss.dbeaver.ext.oracle.model.OracleCompileUnit;
import org.jkiss.dbeaver.ext.oracle.model.OracleSourceObject;
import org.jkiss.dbeaver.ext.oracle.views.OracleCompilerDialog;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CompileHandler extends AbstractHandler implements IElementUpdater
{
    static final Log log = LogFactory.getLog(CompileHandler.class);

    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        final List<OracleCompileUnit> objects = getSelectedObjects(event);
        if (!objects.isEmpty()) {
            final Shell activeShell = HandlerUtil.getActiveShell(event);
            if (objects.size() == 1) {
                final OracleCompileUnit unit = objects.get(0);
                final CompileLog compileLog = new CompileLog();
                Throwable error = null;
                try {
                    DBeaverCore.getInstance().runInProgressService(new DBRRunnableWithProgress() {
                        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                        {
                            try {
                                OracleCompilerDialog.compileUnit(monitor, compileLog, unit);
                            } catch (DBCException e) {
                                throw new InvocationTargetException(e);
                            }
                        }
                    });
                    if (compileLog.error != null) {
                        error = compileLog.error;
                    }
                } catch (InvocationTargetException e) {
                    error = e.getTargetException();
                } catch (InterruptedException e) {
                    return null;
                }
                if (error != null) {
                    UIUtils.showErrorDialog(activeShell, "Unexpected compilation error", null, error);
                } else if (!CommonUtils.isEmpty(compileLog.errorStack)) {
                    // Show compile errors
                    int line = -1, position = -1;
                    StringBuilder fullMessage = new StringBuilder();
                    for (OracleCompileError oce : compileLog.errorStack) {
                        fullMessage.append(oce.toString()).append(ContentUtils.getDefaultLineSeparator());
                        line = oce.getLine();
                        position = oce.getPosition();
                    }
                    // If compiled object is currently open in editor - try to position on error line
                    final IWorkbenchPart activePart = HandlerUtil.getActiveEditor(event);
                    final OracleCompileUnit sourceObject = RuntimeUtils.getObjectAdapter(activePart, OracleCompileUnit.class);
                    if (sourceObject == unit) {
                        BaseTextEditor textEditor = (BaseTextEditor)activePart.getAdapter(BaseTextEditor.class);
                        if (textEditor != null) {
                            try {
                                final IRegion lineInfo = textEditor.getTextViewer().getDocument().getLineInformation(line - 1);
                                final int offset = lineInfo.getOffset() + position;
                                textEditor.selectAndReveal(offset, 0);
                                //textEditor.setFocus();
                            } catch (BadLocationException e) {
                                log.warn(e);
                                // do nothing
                            }
                            activePart.getSite().getPage().activate(activePart);
                        }
                    }

                    UIUtils.showErrorDialog(activeShell, unit.getName() + " compilation failed", fullMessage.toString());
                } else {
                    UIUtils.showMessageBox(activeShell, "Done", unit.getName() + " compiled successfully", SWT.ICON_INFORMATION);
                }
            } else {
                OracleCompilerDialog dialog = new OracleCompilerDialog(activeShell, objects);
                dialog.open();
            }
        }
        return null;
    }

    private List<OracleCompileUnit> getSelectedObjects(ExecutionEvent event)
    {
        List<OracleCompileUnit> objects = new ArrayList<OracleCompileUnit>();
        final ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
        if (currentSelection instanceof IStructuredSelection && !currentSelection.isEmpty()) {
            for (Iterator<?> iter = ((IStructuredSelection) currentSelection).iterator(); iter.hasNext(); ) {
                final Object element = iter.next();
                final OracleCompileUnit sourceObject = RuntimeUtils.getObjectAdapter(element, OracleCompileUnit.class);
                if (sourceObject != null) {
                    objects.add(sourceObject);
                }
            }
        }
        if (objects.isEmpty()) {
            final IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
            final OracleCompileUnit sourceObject = RuntimeUtils.getObjectAdapter(activePart, OracleCompileUnit.class);
            if (sourceObject != null) {
                objects.add(sourceObject);
            }
        }
        return objects;
    }

    public void updateElement(UIElement element, Map parameters)
    {
        List<OracleCompileUnit> objects = new ArrayList<OracleCompileUnit>();
        IWorkbenchPartSite partSite = (IWorkbenchPartSite) element.getServiceLocator().getService(IWorkbenchPartSite.class);
        if (partSite != null) {
            final ISelectionProvider selectionProvider = partSite.getSelectionProvider();
            if (selectionProvider != null) {
                ISelection selection = selectionProvider.getSelection();
                if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
                    for (Iterator<?> iter = ((IStructuredSelection) selection).iterator(); iter.hasNext(); ) {
                        final Object item = iter.next();
                        final OracleCompileUnit sourceObject = RuntimeUtils.getObjectAdapter(item, OracleCompileUnit.class);
                        if (sourceObject != null) {
                            objects.add(sourceObject);
                        }
                    }
                }
            }
            if (objects.isEmpty()) {
                final IWorkbenchPart activePart = partSite.getPart();
                final OracleCompileUnit sourceObject = RuntimeUtils.getObjectAdapter(activePart, OracleCompileUnit.class);
                if (sourceObject != null) {
                    objects.add(sourceObject);
                }
            }
        }
        if (!objects.isEmpty()) {
            if (objects.size() > 1) {
                element.setText("Compile " + objects.size() + " objects");
            } else {
                final OracleCompileUnit sourceObject = objects.get(0);
                String objectType = sourceObject instanceof OracleSourceObject ?
                    CommonUtils.formatWord(((OracleSourceObject) sourceObject).getSourceType().name()) :
                    "";
                element.setText("Compile " + objectType/* + " '" + sourceObject.getName() + "'"*/);
            }
        }
    }

    private class CompileLog extends SimpleLog {
        private Throwable error;
        private List<OracleCompileError> errorStack = new ArrayList<OracleCompileError>();
        public CompileLog()
        {
            super("Compile log");
        }

        @Override
        protected void log(final int type, final Object message, final Throwable t)
        {
            if (t != null) {
                error = t;
            } else if (message instanceof OracleCompileError) {
                errorStack.add((OracleCompileError) message);
            }
        }
    }

}