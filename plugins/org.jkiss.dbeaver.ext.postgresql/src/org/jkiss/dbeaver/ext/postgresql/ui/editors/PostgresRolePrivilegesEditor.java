/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreObject;
import org.jkiss.dbeaver.ext.postgresql.model.PostgrePermission;
import org.jkiss.dbeaver.ext.postgresql.model.PostgrePermissionsOwner;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
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
        PageControl(Composite parent) {
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
            PermissionLoadService() {
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