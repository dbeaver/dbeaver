/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.iotdb.ui.controls;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.iotdb.model.IoTDBGrant;
import org.jkiss.dbeaver.ext.iotdb.model.IoTDBPrivilege;
import org.jkiss.dbeaver.ext.iotdb.ui.internal.IoTDBUiMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomCheckboxCellEditor;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class PrivilegeTableControl extends Composite {

    private TableViewer tableViewer;
    private ViewerColumnController<Object, Object> columnsController;
    private Table privTable;
    private List<IoTDBPrivilege> privileges;
    private List<IoTDBObjectPrivilege> currentPrivileges = new ArrayList<>();

    public PrivilegeTableControl(Composite parent, String title) {
        super(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        gl.verticalSpacing = 0;
        gl.horizontalSpacing = 0;
        setLayout(gl);

        Composite privsGroup = UIUtils.createTitledComposite(this, title, 1, GridData.FILL_BOTH);
        GridData gd = (GridData) privsGroup.getLayoutData();
        gd.horizontalSpan = 2;

        tableViewer = new TableViewer(privsGroup, SWT.BORDER | SWT.UNDERLINE_SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);

        privTable = tableViewer.getTable();
        privTable.setHeaderVisible(true);
        privTable.setLinesVisible(true);
        gd = new GridData(GridData.FILL_BOTH);
        gd.minimumWidth = 300;
        privTable.setLayoutData(gd);

        columnsController = new ViewerColumnController<>("IoTDBPrivilegesEditor", tableViewer); //$NON-NLS-1$
        initColumns();
        columnsController.createColumns(false);
        tableViewer.setContentProvider(new ListContentProvider());

        Composite buttonsPanel = UIUtils.createComposite(privsGroup, 3);
        buttonsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createCheckAllButton(buttonsPanel);
        createClearAllButton(buttonsPanel);
    }

    private void initColumns() {
        addPrivilegeNameColumn();
        addPrivilegeEnabledColumn();
        addPrivilegeGrantColumn();
    }

    private void addPrivilegeNameColumn() {
        columnsController.addColumn(
                IoTDBUiMessages.controls_privilege_table_column_privilege_name,
                IoTDBUiMessages.controls_privilege_table_column_privilege_name_tip,
                SWT.LEFT, true, true, new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                Object element = cell.getElement();
                if (element instanceof IoTDBObjectPrivilege) {
                    cell.setText(((IoTDBObjectPrivilege) element).privilege.getName());
                }
            }
        });
    }

    private void addPrivilegeEnabledColumn() {
        columnsController.addBooleanColumn(
                IoTDBUiMessages.controls_privilege_table_column_privilege_status,
                IoTDBUiMessages.controls_privilege_table_column_privilege_status_tip,
                SWT.CENTER, true, true, item -> {
            if (item instanceof IoTDBObjectPrivilege) {
                return ((IoTDBObjectPrivilege) item).enabled;
            }
            return false;
        }, new EditingSupport(tableViewer) {
            @Override
            protected CellEditor getCellEditor(Object element) {
                return new CustomCheckboxCellEditor(tableViewer.getTable(), true);
            }

            @Override
            protected boolean canEdit(Object element) {
                return true;
            }

            @Override
            protected Object getValue(Object element) {
                if (element instanceof IoTDBObjectPrivilege) {
                    return ((IoTDBObjectPrivilege) element).enabled;
                }
                return false;
            }

            @Override
            protected void setValue(Object element, Object value) {
                if (element instanceof IoTDBObjectPrivilege) {
                    IoTDBObjectPrivilege elementPriv = (IoTDBObjectPrivilege) element;
                    boolean prevC = elementPriv.enabled;
                    if (elementPriv.enabled != Boolean.TRUE.equals(value)) { // handle double click on the box cell
                        elementPriv.enabled = Boolean.TRUE.equals(value);
                        if (!elementPriv.enabled) { // disabled privilege should not have grant option
                            elementPriv.withGrantOption = false;
                        }
                    }
                    boolean newC = elementPriv.enabled;
                    boolean newG = elementPriv.withGrantOption;
                    notifyPrivilegeCheck(elementPriv.privilege, prevC, newC, newG);
                }
            }
        });
    }

    private void addPrivilegeGrantColumn() {
        columnsController.addBooleanColumn(
                IoTDBUiMessages.controls_privilege_table_column_privilege_grant,
                IoTDBUiMessages.controls_privilege_table_column_privilege_grant_tip,
                SWT.CENTER, true, true, item -> {
            if (item instanceof IoTDBObjectPrivilege) {
                return ((IoTDBObjectPrivilege) item).withGrantOption;
            }
            return false;
        }, new EditingSupport(tableViewer) {
            @Override
            protected CellEditor getCellEditor(Object element) {
                return new CustomCheckboxCellEditor(tableViewer.getTable(), true);
            }

            @Override
            protected boolean canEdit(Object element) {
                if (element instanceof IoTDBObjectPrivilege) {
                    return ((IoTDBObjectPrivilege) element).enabled;
                }
                return false;
            }

            @Override
            protected Object getValue(Object element) {
                if (element instanceof IoTDBObjectPrivilege) {
                    return ((IoTDBObjectPrivilege) element).withGrantOption;
                }
                return false;
            }

            @Override
            protected void setValue(Object element, Object value) {
                if (element instanceof IoTDBObjectPrivilege) {
                    IoTDBObjectPrivilege elementPriv = (IoTDBObjectPrivilege) element;
                    boolean prevC = elementPriv.enabled;
                    if (elementPriv.withGrantOption != Boolean.TRUE.equals(value)) {
                        elementPriv.withGrantOption = Boolean.TRUE.equals(value);
                    }
                    boolean newG = elementPriv.withGrantOption;
                    notifyPrivilegeCheck(elementPriv.privilege, prevC, prevC, newG);
                }
            }
        });
    }

    private void createCheckAllButton(Composite buttonsPanel) {
        UIUtils.createPushButton(buttonsPanel,
                IoTDBUiMessages.controls_privilege_table_push_button_check_all, null, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                for (IoTDBObjectPrivilege userPrivilege : CommonUtils.safeCollection(currentPrivileges)) {
                    userPrivilege.enabled = true;
                    userPrivilege.withGrantOption = true;
                    notifyPrivilegeCheck(userPrivilege.privilege, true, true, true);
                }
                drawColumns(currentPrivileges);
            }
        });
    }

    private void createClearAllButton(Composite buttonsPanel) {
        UIUtils.createPushButton(buttonsPanel,
                IoTDBUiMessages.controls_privilege_table_push_button_clear_all, null, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                for (IoTDBObjectPrivilege userPrivilege : CommonUtils.safeCollection(currentPrivileges)) {
                    userPrivilege.enabled = false;
                    userPrivilege.withGrantOption = false;
                    notifyPrivilegeCheck(userPrivilege.privilege, true, false, false);
                }
                drawColumns(currentPrivileges);
            }
        });
    }

    /**
     * Notify privilege check
     * grant withGrantOption --> grant withGrantOption
     * 0 0 --> 1 0, 1 1
     * 0 1 invalid
     * 1 0 --> 0 0, 1 1
     * 1 1 --> 0 0, 1 0
     * --------------------
     * 1 1 --> 1 0 revoke grant [0]
     * 0 0 --> 1 0 grant [1]
     * 0 0 --> 1 1, 1 0 --> 1 1 grant with option [2]
     * 1 0 --> 0 0, 1 1 --> 0 0 revoke [3]
     *
     * @param privilege IoTDBPrivilege
     * @param checked boolean
     * @param withGrantOption boolean
     */
    private void notifyPrivilegeCheck(IoTDBPrivilege privilege, boolean prevC, boolean checked, boolean withGrantOption) {
        Event event = new Event();

        if (checked && withGrantOption) {
            event.detail = 2;
        } else if (!checked && !withGrantOption) {
            event.detail = 3;
        } else if (prevC) {
            event.detail = 0;
        } else event.detail = 1;

        event.widget = this;
        event.data = privilege;
        super.notifyListeners(SWT.Modify, event);
    }

    /**
     * Fill privileges to class variable
     *
     * @param tempPrivileges List of IoTDBPrivilege
     */
    public void fillPrivileges(@NotNull List<IoTDBPrivilege> tempPrivileges) {
        this.privileges = new ArrayList<>(tempPrivileges);
    }

    public void fillGrants(List<IoTDBGrant> grants, boolean editable) {
        privTable.setEnabled(editable);
        fillGrants(grants);
    }

    /**
     * Fill grants to Privileges Tables
     *
     * @param grants List of IoTDBGrant
     */
    public void fillGrants(List<IoTDBGrant> grants) {
        if (CommonUtils.isEmpty(privileges)) {
            return;
        }
        currentPrivileges = new ArrayList<>();
        if (CommonUtils.isEmpty(grants)) {
            for (IoTDBPrivilege privilege : privileges) {
                currentPrivileges.add(new IoTDBObjectPrivilege(privilege, false, false));
            }
            drawColumns(currentPrivileges);
            return;
        }

        boolean privilegeEnabled;
        boolean privilegeGranted;
        for (IoTDBPrivilege privilege : privileges) {
            privilegeEnabled = false;
            privilegeGranted = false;

            for (IoTDBGrant grant : grants) {
                if (ArrayUtils.contains(grant.getPrivileges(), privilege)) {
                    privilegeEnabled = true;

                    if (grant.isGranted()) {
                        privilegeGranted = true;
                    }
                }
            }
            currentPrivileges.add(new IoTDBObjectPrivilege(privilege, privilegeEnabled, privilegeGranted));
        }

        drawColumns(currentPrivileges);
    }

    private void drawColumns(List<?> objects) {
        tableViewer.setInput(objects);
        tableViewer.refresh();
        columnsController.repackColumns();
    }

    public void checkPrivilege(IoTDBPrivilege privilege, boolean grant) {
        for (IoTDBObjectPrivilege basePrivilege : currentPrivileges) {
            if (basePrivilege.privilege == privilege) {
                basePrivilege.enabled = grant;
            }
        }
        drawColumns(currentPrivileges);
    }

    private static class IoTDBObjectPrivilege {

        private IoTDBPrivilege privilege;
        private boolean enabled;
        private boolean withGrantOption;

        IoTDBObjectPrivilege(IoTDBPrivilege privilege, boolean enabled, boolean withGrantOption) {
            this.privilege = privilege;
            this.enabled = enabled;
            this.withGrantOption = withGrantOption;
        }
    }
}
