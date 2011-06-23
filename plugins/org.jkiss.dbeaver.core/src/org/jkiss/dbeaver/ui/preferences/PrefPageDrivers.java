/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.qm.QMConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;

/**
 * PrefPageDrivers
 */
public class PrefPageDrivers extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.drivers";
    private TableViewer libTable;

    public PrefPageDrivers()
    {
        super();
        setPreferenceStore(DBeaverCore.getInstance().getGlobalPreferenceStore());
    }

    public void init(IWorkbench workbench)
    {

    }

    @Override
    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        //ListViewer list = new ListViewer(libsGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        {
            Composite libsListGroup = new Composite(composite, SWT.NONE);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 200;
            libsListGroup.setLayoutData(gd);
            GridLayout layout = new GridLayout(1, false);
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            libsListGroup.setLayout(layout);
            //gd = new GridData(GridData.FILL_HORIZONTAL);

            // Additional libraries list
            libTable = new TableViewer(libsListGroup, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
            //libsTable.setLinesVisible (true);
            //libsTable.setHeaderVisible (true);
            libTable.setContentProvider(new ListContentProvider());
            libTable.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell)
                {
//                    DriverFileDescriptor lib = (DriverFileDescriptor) cell.getElement();
//                    cell.setText(lib.getPath());
//                    if (lib.getFile().exists()) {
//                        cell.setForeground(null);
//                    } else {
//                        cell.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
//                    }
//                    cell.setImage(
//                        !lib.getFile().exists() ?
//                            PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE) :
//                            lib.getFile().isDirectory() ?
//                                DBIcon.TREE_FOLDER.getImage() :
//                                DBIcon.JAR.getImage());
                }
            });
            libTable.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
            libTable.getControl().addListener(SWT.Selection, new Listener()
            {
                public void handleEvent(Event event)
                {

                }
            });

//            libList = new ArrayList<DriverFileDescriptor>();
//            for (DriverFileDescriptor lib : driver.getFiles()) {
//                if (lib.isDisabled() || lib.getType() != DriverFileType.jar) {
//                    continue;
//                }
//                libList.add(lib);
//            }
//            libTable.setInput(libList);
        }

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();
        java.util.List<String> pathList = CommonUtils.splitString(store.getString(PrefConstants.NATIVE_LIB_PATH), ';');

        super.performDefaults();
    }

    @Override
    public boolean performOk()
    {
        java.util.List<String> objectTypes = new ArrayList<String>();

        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();
        store.setValue(QMConstants.PROP_OBJECT_TYPES, CommonUtils.makeString(objectTypes, ','));
        RuntimeUtils.savePreferenceStore(store);

        return super.performOk();
    }

    public IAdaptable getElement()
    {
        return null;
    }

    public void setElement(IAdaptable element)
    {

    }

}