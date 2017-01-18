/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql.ui.editors;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.controls.itemlist.DatabaseObjectListControl;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

/**
 * PostgresRolePrivilegesEditor
 */
public class PostgresRolePrivilegesEditor extends AbstractDatabaseObjectEditor<PostgrePermissionsOwner>
{
    private static final Log log = Log.getLog(PostgresRolePrivilegesEditor.class);

    private PageControl pageControl;

    private Font boldFont;
    private boolean isLoaded;

    @Override
    public void createPartControl(Composite parent)
    {
        this.boldFont = UIUtils.makeBoldFont(parent.getFont());
        parent.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                UIUtils.dispose(boldFont);
            }
        });

        this.pageControl = new PageControl(parent);
        this.pageControl.createOrSubstituteProgressPanel(getSite());
        this.pageControl.setDoubleClickHandler(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                final ISelection selection = pageControl.getSelectionProvider().getSelection();
                if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
                    final PostgrePermission element = (PostgrePermission) ((IStructuredSelection) selection).getFirstElement();
                    new AbstractJob("Open target object") {
                        @Override
                        protected IStatus run(DBRProgressMonitor monitor) {
                            try {
                                final PostgreObject targetObject = element.getTargetObject(monitor);
                                if (targetObject != null) {
                                    DBeaverUI.syncExec(new Runnable() {
                                        @Override
                                        public void run() {
                                            NavigatorHandlerObjectOpen.openEntityEditor(targetObject);
                                        }
                                    });
                                }
                            } catch (DBException e) {
                                return GeneralUtils.makeExceptionStatus(e);
                            }
                            return Status.OK_STATUS;
                        }
                    }.schedule();
                }
            }
        });
    }

    private boolean isRoleEditor() {
        return getDatabaseObject() instanceof PostgreRole;
    }
    @Override
    public void setFocus() {
        if (this.pageControl != null) {
            this.pageControl.setFocus();
        }
    }

    @Override
    public synchronized void activatePart()
    {
        if (isLoaded) {
            return;
        }
        isLoaded = true;
        pageControl.loadData();
    }

    @Override
    public void refreshPart(Object source, boolean force)
    {
        pageControl.loadData();
    }

    private class PermissionsContentProvider extends TreeContentProvider {
        @Override
        public Object[] getChildren(Object parentElement) {
            return null;
        }

        @Override
        public boolean hasChildren(Object element) {
            return false;
        }
    }

    private class PageControl extends DatabaseObjectListControl<PostgrePermission> {
        public PageControl(Composite parent) {
            super(parent, SWT.SHEET, getSite(), new PermissionsContentProvider());

        }

        @NotNull
        @Override
        protected String getListConfigId(List<Class<?>> classList) {
            return "PostgreSQL/Permissions/" + (isRoleEditor() ? "Role" : "Object");
        }

        @Override
        protected Class<?>[] getListBaseTypes(Collection<PostgrePermission> items) {
            return new Class[] {PostgrePermission.class};
        }

        @Nullable
        @Override
        protected DBPImage getObjectImage(PostgrePermission item) {
            return isRoleEditor() ? DBIcon.TREE_TABLE : DBIcon.TREE_USER;
        }

        @Override
        protected LoadingJob<Collection<PostgrePermission>> createLoadService() {
            return LoadingJob.createService(
                new PermissionLoadService(),
                new PermissionLoadVisualizer());
        }

        public class PermissionLoadVisualizer extends ObjectsLoadVisualizer {
            @Override
            public void completeLoading(Collection<PostgrePermission> items)
            {
                super.completeLoading(items);
            }
        }

        private class PermissionLoadService extends DatabaseLoadService<Collection<PostgrePermission>> {
            public PermissionLoadService() {
                super("Load privileges", getExecutionContext());
            }

            @Override
            public Collection<PostgrePermission> evaluate(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                try {
                    return getDatabaseObject().getPermissions(monitor);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            }
        }
    }


}