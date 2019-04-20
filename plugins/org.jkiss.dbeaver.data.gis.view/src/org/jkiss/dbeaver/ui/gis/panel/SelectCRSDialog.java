/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.gis.panel;

import org.cts.CRSFactory;
import org.cts.crs.CoordinateReferenceSystem;
import org.cts.registry.RegistryException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.gis.GisTransformUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.internal.UIActivator;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

/**
 * Database select dialog
 */
public class SelectCRSDialog extends BaseDialog {

    private static final Log log = Log.getLog(SelectCRSDialog.class);

    private static final String DIALOG_ID = "DBeaver.SelectCRSDialog";//$NON-NLS-1$

    private int selectedSRID;
    private static CRSLoader crsLoader;

    private static class CRSInfo {
        int code;
        String name;
        String coordSystemName;
        String projectionName;
    }

    public SelectCRSDialog(Shell shell, int defCRS) {
        super(shell, "Select Coordinate Reference System (CRS)", null);
        selectedSRID = defCRS;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getSettingsSection(UIActivator.getDefault().getDialogSettings(), DIALOG_ID);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);

        if (crsLoader == null) {
            crsLoader = new CRSLoader();
            try {
                UIUtils.runInProgressDialog(crsLoader);
            } catch (InvocationTargetException e) {
                log.error("Error loading CRS list", e.getTargetException());
            }
        }

        PatternFilter patternFilter = new PatternFilter() {

            @Override
            protected boolean isLeafMatch(Viewer viewer, Object element) {
                if (element instanceof CRSInfo) {
                    return wordMatches(((CRSInfo) element).name) ||
                        wordMatches(String.valueOf(((CRSInfo) element).code)) ||
                        wordMatches(((CRSInfo) element).coordSystemName) ||
                        wordMatches(((CRSInfo) element).projectionName);
                }
                return super.isLeafMatch(viewer, element);
            }
        };
        FilteredTree filteredTree = new FilteredTree(dialogArea, SWT.BORDER | SWT.FULL_SELECTION, patternFilter, true);
        TreeViewer treeViewer = filteredTree.getViewer();
        Tree crsTree = treeViewer.getTree();
        crsTree.setLayoutData(new GridData(GridData.FILL_BOTH));
        crsTree.setHeaderVisible(true);

        UIUtils.createTreeColumn(crsTree, SWT.LEFT, "Name");
        UIUtils.createTreeColumn(crsTree, SWT.LEFT, "SRID");
        UIUtils.createTreeColumn(crsTree, SWT.LEFT, "Coord. System");
        UIUtils.createTreeColumn(crsTree, SWT.LEFT, "Projection");

        treeViewer.setContentProvider(new ITreeContentProvider() {
            @Override
            public void dispose() {

            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

            }

            @Override
            public Object[] getChildren(Object parentElement) {
                if (parentElement == crsLoader) {
                    return crsLoader.crsMap.keySet().toArray(new Object[0]);
                } else if (parentElement instanceof String) {
                    List<CRSInfo> crsInfos = crsLoader.crsMap.get(parentElement);
                    return crsInfos == null ? null : crsInfos.toArray(new Object[0]);
                } else {
                    return new Object[0];
                }
            }

            @Override
            public Object getParent(Object element) {
                if (element instanceof String) {
                    return crsTree;
                } else {
                    return null;
                }
            }

            @Override
            public boolean hasChildren(Object element) {
                if (element == crsLoader) {
                    return !crsLoader.crsMap.isEmpty();
                } else if (element instanceof String) {
                    List<CRSInfo> crsInfos = crsLoader.crsMap.get(element);
                    return crsInfos != null && !crsInfos.isEmpty();
                } else {
                    return false;
                }
            }

            @Override
            public Object[] getElements(Object inputElement) {
                return getChildren(inputElement);
            }
        });
        treeViewer.setLabelProvider(new CRSLabelProvider());
        treeViewer.setInput(crsLoader);
        treeViewer.expandAll();
        UIUtils.packColumns(treeViewer.getTree(), true, null);

/*
        for (String regName : crsLoader.crsMap.keySet()) {
            regName = regName.toUpperCase(Locale.ENGLISH);
            TreeItem regItem = new TreeItem(crsTree, SWT.NONE);
            regItem.setText(0, regName);
            for (CRSInfo crsInfo : crsLoader.crsMap.get(regName)) {
                TreeItem crsItem = new TreeItem(regItem, SWT.NONE);
                crsItem.setText(0, crsInfo.name);
                crsItem.setText(1, String.valueOf(crsInfo.code));
                crsItem.setText(2, crsInfo.coordSystemName);
                crsItem.setText(3, crsInfo.projectionName);
                crsItem.setData(crsInfo);
            }
            regItem.setExpanded(true);
        }
*/

        UIUtils.packColumns(crsTree, true, null);

        return dialogArea;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);
        updateButtons();
        return contents;
    }

    private void updateButtons() {
        getButton(IDialogConstants.OK_ID).setEnabled(selectedSRID != 0);
    }

    public int getSelectedSRID() {
        return selectedSRID;
    }

    private static class CRSLabelProvider extends CellLabelProvider implements ILabelProvider {
        @Override
        public void update(ViewerCell cell) {
            Object element = cell.getElement();
            if (element instanceof String) {
                if (cell.getColumnIndex() == 0) {
                    cell.setText(element.toString());
                }
            } else {
                CRSInfo crsInfo = (CRSInfo) element;
                switch (cell.getColumnIndex()) {
                    case 0: {
                        cell.setText(crsInfo.name);
                        break;
                    }
                    case 1: {
                        cell.setText(String.valueOf(crsInfo.code));
                        break;
                    }
                    case 2: {
                        cell.setText(crsInfo.coordSystemName);
                        break;
                    }
                    case 3: {
                        cell.setText(crsInfo.projectionName);
                        break;
                    }
                }
            }
        }

        @Override
        public Image getImage(Object element) {
            return null;
        }

        @Override
        public String getText(Object element) {
            if (element instanceof String) {
                return element.toString();
            } else if (element instanceof CRSInfo) {
                return ((CRSInfo) element).name;
            }
            return "";
        }
    }

    private class CRSLoader implements DBRRunnableWithProgress {

        private Map<String, List<CRSInfo>> crsMap = new LinkedHashMap<>();

        @Override
        public void run(DBRProgressMonitor monitor) {
            CRSFactory crsFactory = GisTransformUtils.getCRSFactory();
            String[] allRegistries = crsFactory.getRegistryManager().getRegistryNames();
            for (String regName : allRegistries) {
                regName = regName.toUpperCase(Locale.ENGLISH);
                try {
                    List<Integer> crsCodes = new ArrayList<>();

                    for (String code : crsFactory.getSupportedCodes(regName)) {
                        crsCodes.add(CommonUtils.toInt(code));
                    }
                    crsCodes.sort(Integer::compareTo);
                    monitor.beginTask("Load CRS list", crsCodes.size());
                    for (Integer code : crsCodes) {
                        String crsID = regName + ":" + code;
                        monitor.subTask("Load CRS " + crsID);
                        try {
                            CoordinateReferenceSystem crs = crsFactory.getCRS(crsID);

                            List<CRSInfo> crsInfoList = crsMap.computeIfAbsent(regName, s -> new ArrayList<>());
                            CRSInfo crsInfo = new CRSInfo();
                            crsInfo.code = code;
                            crsInfo.name = crs.getName();
                            crsInfo.coordSystemName = crs.getCoordinateSystem().toString();
                            crsInfo.projectionName = crs.getProjection() == null ? "" : crs.getProjection().getName();
                            crsInfoList.add(crsInfo);
                        } catch (Exception e) {
                            log.debug("Error loading CRS " + code + ": " + e.getMessage());
                        }
                        monitor.worked(1);
                        if (monitor.isCanceled()) {
                            break;
                        }
                    }
                    monitor.done();
                } catch (RegistryException e) {
                    log.error("Error reading SRID list", e);
                }
            }
        }
    }

}