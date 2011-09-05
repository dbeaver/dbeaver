/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.ext.oracle.model.OracleSourceEditable;
import org.jkiss.dbeaver.ext.oracle.model.OracleSourceObject;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CompileHandler extends AbstractHandler implements IElementUpdater
{
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        List<OracleSourceEditable> objects = getSelectedObjects(event);
        if (!objects.isEmpty()) {

        }
        return null;
    }

    private List<OracleSourceEditable> getSelectedObjects(ExecutionEvent event)
    {
        List<OracleSourceEditable> objects = new ArrayList<OracleSourceEditable>();
        final ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
        if (currentSelection instanceof IStructuredSelection && !currentSelection.isEmpty()) {
            for (Iterator<?> iter = ((IStructuredSelection) currentSelection).iterator(); iter.hasNext(); ) {
                final Object element = iter.next();
                final OracleSourceEditable sourceObject = RuntimeUtils.getObjectAdapter(element, OracleSourceEditable.class);
                if (sourceObject != null) {
                    objects.add(sourceObject);
                }
            }
        }
        if (objects.isEmpty()) {
            final IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
            final OracleSourceEditable sourceObject = RuntimeUtils.getObjectAdapter(activePart, OracleSourceEditable.class);
            if (sourceObject != null) {
                objects.add(sourceObject);
            }
        }
        return objects;
    }

    public void updateElement(UIElement element, Map parameters)
    {
        List<OracleSourceEditable> objects = new ArrayList<OracleSourceEditable>();
        IWorkbenchPartSite partSite = (IWorkbenchPartSite) element.getServiceLocator().getService(IWorkbenchPartSite.class);
        if (partSite != null) {
            final ISelectionProvider selectionProvider = partSite.getSelectionProvider();
            if (selectionProvider != null) {
                ISelection selection = selectionProvider.getSelection();
                if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
                    for (Iterator<?> iter = ((IStructuredSelection) selection).iterator(); iter.hasNext(); ) {
                        final Object item = iter.next();
                        final OracleSourceEditable sourceObject = RuntimeUtils.getObjectAdapter(item, OracleSourceEditable.class);
                        if (sourceObject != null) {
                            objects.add(sourceObject);
                        }
                    }
                }
            }
            if (objects.isEmpty()) {
                final IWorkbenchPart activePart = partSite.getPart();
                final OracleSourceEditable sourceObject = RuntimeUtils.getObjectAdapter(activePart, OracleSourceEditable.class);
                if (sourceObject != null) {
                    objects.add(sourceObject);
                }
            }
        }
        if (!objects.isEmpty()) {
            if (objects.size() > 1) {
                element.setText("Compile " + objects.size() + " objects");
            } else {
                final OracleSourceObject sourceObject = objects.get(0);
                element.setText("Compile " + CommonUtils.formatWord(sourceObject.getSourceType().name())/* + " '" + sourceObject.getName() + "'"*/);
            }
        }
    }

}