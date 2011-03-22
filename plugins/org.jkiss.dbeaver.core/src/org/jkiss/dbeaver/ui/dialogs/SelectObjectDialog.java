/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.itemlist.ObjectListControl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * SelectObjectDialog
 *
 * @author Serge Rieder
 */
public class SelectObjectDialog<T> extends Dialog {

    private String title;
    private Collection<T> objects;
    private List<T> selectedObjects = new ArrayList<T>();
    private boolean singleSelection;

    private SelectObjectDialog(Shell parentShell, String title, boolean singleSelection, Collection<T> objects)
    {
        super(parentShell);
        this.title = title;
        this.singleSelection = singleSelection;
        this.objects = objects;
    }

    protected boolean isResizable()
    {
        return true;
    }

    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(title);

        Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(gd);

        ObjectListControl<T> objectList = new ObjectListControl<T>(group, SWT.BORDER | (singleSelection ? SWT.SINGLE : SWT.MULTI)) {
            @Override
            protected IContentProvider createContentProvider()
            {
                return new ListContentProvider();
            }
        };
        gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 300;
        gd.minimumWidth = 300;
        objectList.setLayoutData(gd);
        objectList.getItemsViewer().addSelectionChangedListener(new ISelectionChangedListener() {
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
            public void doubleClick(DoubleClickEvent event)
            {
                if (getButton(IDialogConstants.OK_ID).isEnabled()) {
                    okPressed();
                }
            }
        });

        LoadingJob<Collection<T>> loadingJob = LoadingUtils.createService(
            new AbstractLoadService<Collection<T>>() {
                public Collection<T> evaluate() throws InvocationTargetException, InterruptedException
                {
                    return objects;
                }

                public Object getFamily()
                {
                    return SelectObjectDialog.this;
                }
            },
            objectList.createVisualizer());
        objectList.loadData(loadingJob);

        return group;
    }

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

    public static <T> List<T> selectObjects(Shell parentShell, String title, Collection<T> objects)
    {
        SelectObjectDialog<T> scDialog = new SelectObjectDialog<T>(parentShell, title, false, objects);
        if (scDialog.open() == IDialogConstants.OK_ID) {
            return scDialog.getSelectedObjects();
        } else {
            return null;
        }
    }

    public static <T> T selectObject(Shell parentShell, String title, Collection<T> objects)
    {
        SelectObjectDialog<T> scDialog = new SelectObjectDialog<T>(parentShell, title, true, objects);
        if (scDialog.open() == IDialogConstants.OK_ID) {
            final List<T> selectedObjects = scDialog.getSelectedObjects();
            return CommonUtils.isEmpty(selectedObjects) ? null : selectedObjects.get(0);
        } else {
            return null;
        }
    }

}
