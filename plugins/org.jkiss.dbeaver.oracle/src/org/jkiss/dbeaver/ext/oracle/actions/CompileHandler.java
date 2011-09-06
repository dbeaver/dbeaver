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
import org.jkiss.dbeaver.ext.oracle.model.OracleCompileUnit;
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
        List<OracleCompileUnit> objects = getSelectedObjects(event);
        if (!objects.isEmpty()) {

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

}