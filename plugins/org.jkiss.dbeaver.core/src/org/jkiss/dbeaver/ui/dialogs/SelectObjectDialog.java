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
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.itemlist.DatabaseObjectListControl;
import org.jkiss.dbeaver.ui.controls.itemlist.ObjectListControl;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
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
    private Font boldFont;
    private boolean modeless;

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
        this.boldFont = UIUtils.makeBoldFont(parentShell.getFont());
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID + "." + listId);
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    public void setModeless(boolean modeless) {
        this.modeless = modeless;
        if (modeless) {
            setShellStyle(SWT.SHELL_TRIM);
        } else {
            setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.MAX | SWT.RESIZE);
        }
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(title);

        Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(gd);

        final DatabaseObjectListControl<T> objectList = new DatabaseObjectListControl<T>(
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

            protected CellLabelProvider getColumnLabelProvider(ObjectColumn objectColumn) {
                return new ObjectLabelProvider(objectColumn);
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
                    getItemsViewer().setSelection(new StructuredSelection(selectedObjects), true);
                }
            }

            class ObjectLabelProvider extends ObjectColumnLabelProvider implements IFontProvider {
                ObjectLabelProvider(ObjectColumn objectColumn) {
                    super(objectColumn);
                }

                @Override
                public Font getFont(Object element)
                {
                    if (selectedObjects.contains(element)) {
                        return boldFont;
                    }
                    return null;
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
                if (!modeless) {
                    getButton(IDialogConstants.OK_ID).setEnabled(!selectedObjects.isEmpty());
                }
            }
        });
        objectList.setDoubleClickHandler(event -> {
            if (modeless || getButton(IDialogConstants.OK_ID).isEnabled()) {
                okPressed();
            }
        });

        objectList.loadData();

        Control listControl = objectList.getItemsViewer().getControl();
        listControl.setFocus();
        if (modeless) {
            listControl.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    DBeaverUI.asyncExec(() -> {
                        if (!UIUtils.isParent(getShell(), getShell().getDisplay().getFocusControl())) {
                            cancelPressed();
                        }
                    });
                }
            });
        }

        return group;
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        if (this.modeless) {
            return UIUtils.createPlaceholder(parent, 1);
        }
        return super.createButtonBar(parent);
    }

    @Override
    public int open() {
        int result = super.open();

        UIUtils.dispose(boldFont);

        return result;
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Control ctl = super.createContents(parent);
        if (!modeless) {
            getButton(IDialogConstants.OK_ID).setEnabled(false);
        }
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
