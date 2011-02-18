/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.WorkbenchPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;
import org.jkiss.dbeaver.ui.views.search.SearchObjectsView;

@SuppressWarnings("restriction")
public class NavigatorHandlerObjectFind extends DataSourceHandler {

    private static final String VIEW_ACTIVATED = "search.objects.view.activates";

    public Object execute(ExecutionEvent event) throws ExecutionException {
        WorkbenchPage activePage = (WorkbenchPage)HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
        if (activePage == null) {
            return null;
        }

        try {
            IViewPart view = activePage.findView(SearchObjectsView.VIEW_ID);
            if (view == null) {
                SearchObjectsView searchView = (SearchObjectsView)activePage.showView(SearchObjectsView.VIEW_ID);
                searchView.setCurrentDataSource(getDataSourceContainer(event, true, false));
                searchView.afterCreate();

                IViewReference viewReference = (IViewReference) activePage.getReference(searchView);
                if (!DBeaverCore.getInstance().getGlobalPreferenceStore().getBoolean(VIEW_ACTIVATED)) {
                    activePage.detachView(viewReference);
                    Shell viewShell = searchView.getViewSite().getShell();
                    Rectangle windowBounds = activePage.getWorkbenchWindow().getShell().getBounds();
                    viewShell.setBounds(windowBounds.x + 100, windowBounds.y + 100, 800, 650);
                    DBeaverCore.getInstance().getGlobalPreferenceStore().setValue(VIEW_ACTIVATED, true);
                }
                //activePage.setState();
            } else {
                activePage.hideView(view);
            }
        } catch (PartInitException ex) {
            log.error("Can't open search view");
        }
        return null;
    }

}