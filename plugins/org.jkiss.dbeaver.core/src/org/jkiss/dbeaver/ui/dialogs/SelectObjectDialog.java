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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.itemlist.DatabaseObjectListControl;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * SelectObjectDialog
 *
 * @author Serge Rider
 */
public class SelectObjectDialog<T extends DBPObject> extends Dialog {

    private static final String DIALOG_ID = "DBeaver.SelectObjectDialog";//$NON-NLS-1$

    private String title;
    private String listId;
    private Collection<T> objects;
    private List<T> selectedObjects = new ArrayList<>();
    private boolean singleSelection;

    public SelectObjectDialog(Shell parentShell, String title, boolean singleSelection, String listId, Collection<T> objects, Collection<T> selected)
    {
        super(parentShell);
        this.title = title;
        this.singleSelection = singleSelection;
        this.listId = listId;
        this.objects = new ArrayList<>(objects);
        if (selected != null) {
            selectedObjects.addAll(selected);
        }
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(title);

        Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(gd);

        DatabaseObjectListControl<T> objectList = new DatabaseObjectListControl<T>(
            group,
            (singleSelection ? SWT.SINGLE : SWT.MULTI),
            null,
            new ListContentProvider())
        {
            @NotNull
            @Override
            protected String getListConfigId(List<Class<?>> classList) {
                return listId;
            }

            @Override
            protected LoadingJob<Collection<T>> createLoadService()
            {
                return LoadingJob.createService(
                    new AbstractLoadService<Collection<T>>() {
                        @Override
                        public Collection<T> evaluate(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                            return objects;
                        }

                        @Override
                        public Object getFamily() {
                            return SelectObjectDialog.this;
                        }
                    },
                    new ObjectsLoadVisualizer());
            }

            @Override
            protected Object getObjectValue(T item) {
                if (item instanceof DBSWrapper) {
                    return ((DBSWrapper) item).getObject();
                }
                return super.getObjectValue(item);
            }
            @Override
            protected DBPImage getObjectImage(T item)
            {
                if (item instanceof DBNDatabaseNode) {
                    return ((DBNDatabaseNode) item).getNodeIcon();
                }
                return null;
            }

            @Override
            protected void setListData(Collection<T> items, boolean append) {
                super.setListData(items, append);
                if (selectedObjects != null) {
                    getItemsViewer().setSelection(new StructuredSelection(selectedObjects));
                }
            }
        };
        objectList.createProgressPanel();
        gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 300;
        gd.minimumWidth = 300;
        objectList.setLayoutData(gd);
        objectList.getSelectionProvider().addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event)
            {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                selectedObjects.clear();
                selectedObjects.addAll(selection.toList());
                getButton(IDialogConstants.OK_ID).setEnabled(!selectedObjects.isEmpty());
            }
        });
        objectList.setDoubleClickHandler(new IDoubleClickListener()
        {
            @Override
            public void doubleClick(DoubleClickEvent event)
            {
                if (getButton(IDialogConstants.OK_ID).isEnabled()) {
                    okPressed();
                }
            }
        });

        objectList.loadData();

        return group;
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Control ctl = super.createContents(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
        return ctl;
    }

    public List<T> getSelectedObjects()
    {
        return selectedObjects;
    }

    public T getSelectedObject()
    {
        return selectedObjects.isEmpty() ? null : selectedObjects.get(0);
    }

    public static <T extends DBPObject> T selectObject(Shell parentShell, String title, String listId, Collection<T> objects)
    {
        SelectObjectDialog<T> scDialog = new SelectObjectDialog<>(parentShell, title, true, listId, objects, null);
        if (scDialog.open() == IDialogConstants.OK_ID) {
            final List<T> selectedObjects = scDialog.getSelectedObjects();
            return CommonUtils.isEmpty(selectedObjects) ? null : selectedObjects.get(0);
        } else {
            return null;
        }
    }

}
