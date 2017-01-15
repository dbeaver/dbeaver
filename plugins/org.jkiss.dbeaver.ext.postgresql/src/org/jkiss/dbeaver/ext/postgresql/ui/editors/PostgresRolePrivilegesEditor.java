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

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;

import java.lang.reflect.InvocationTargetException;
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
    private TreeViewer permissionTable;
    private ViewerColumnController columnController;

    @Override
    public void createPartControl(Composite parent)
    {
        boldFont = UIUtils.makeBoldFont(parent.getFont());

        pageControl = new PageControl(parent);
        final Composite contentContainer = pageControl.createContentContainer();

        {
            this.permissionTable = new TreeViewer(contentContainer, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
            this.permissionTable.getTree().setHeaderVisible(true);
            this.permissionTable.setContentProvider(new TreeContentProvider() {
                @Override
                public Object[] getChildren(Object parentElement) {
                    return null;
                }

                @Override
                public boolean hasChildren(Object element) {
                    return false;
                }

            });

            final boolean roleEditor = isRoleEditor();
            columnController = new ViewerColumnController("PostgreSQL/Permissions/" + (roleEditor ? "Role" : "Object"), permissionTable);
            if (roleEditor) {
                columnController.addColumn("Object", "Granted object", SWT.LEFT, true, true, new ColumnLabelProvider() {
                    @Override
                    public Image getImage(Object element) {
                        return DBeaverIcons.getImage(DBIcon.TREE_TABLE);
                    }

                    @Override
                    public String getText(Object element) {
                        return ((PostgreRolePermission) element).getFullTableName();
                    }
                });
            } else {
                columnController.addColumn("Role", "Granted role", SWT.LEFT, true, true, new ColumnLabelProvider() {
                    @Override
                    public Image getImage(Object element) {
                        return DBeaverIcons.getImage(DBIcon.TREE_USER);
                    }

                    @Override
                    public String getText(Object element) {
                        return ((PostgrePermission) element).toString();
                    }
                });
            }
            columnController.addColumn("SELECT", "SELECT permissions", SWT.LEFT, true, true, new ColumnLabelProvider() {
                @Override
                public String getText(Object element) {
                    return "NO";
                }
            });
        }
        columnController.createColumns(true);
        pageControl.createOrSubstituteProgressPanel(getSite());

        parent.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                UIUtils.dispose(boldFont);
            }
        });

    }

    private boolean isRoleEditor() {
        return getDatabaseObject() instanceof PostgreRole;
    }
    @Override
    public void setFocus() {

    }

    @Override
    public synchronized void activatePart()
    {
        if (isLoaded) {
            return;
        }
        isLoaded = true;
        loadPrivileges();
    }

    private void loadPrivileges() {
        LoadingJob.createService(
            new DatabaseLoadService<List<? extends PostgrePermission>>("Load privileges", getExecutionContext()) {
                @Override
                public List<? extends PostgrePermission> evaluate(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        return getDatabaseObject().getPermissions(monitor);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            },
            pageControl.createPrivilegesLoadVisualizer())
            .schedule();
    }

    @Override
    public void refreshPart(Object source, boolean force)
    {
        loadPrivileges();
    }

    private class PageControl extends ObjectEditorPageControl {
        public PageControl(Composite parent) {
            super(parent, SWT.SHEET, PostgresRolePrivilegesEditor.this);
        }

        public ProgressPageControl.ProgressVisualizer<List<? extends PostgrePermission>> createPrivilegesLoadVisualizer() {
            return new ProgressPageControl.ProgressVisualizer<List<? extends PostgrePermission>>() {
                @Override
                public void completeLoading(List<? extends PostgrePermission> privs) {
                    super.completeLoading(privs);
                    permissionTable.setInput(privs);
                }
            };
        }

    }


}