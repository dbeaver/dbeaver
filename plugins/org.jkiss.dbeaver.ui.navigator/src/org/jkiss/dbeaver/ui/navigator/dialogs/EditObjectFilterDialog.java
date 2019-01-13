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
package org.jkiss.dbeaver.ui.navigator.dialogs;

import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.StringEditorTable;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Object filter edit dialog
 */
public class EditObjectFilterDialog extends HelpEnabledDialog {

    public static final int SHOW_GLOBAL_FILTERS_ID = 1000;
    private static final String NULL_FILTER_NAME = "";

    private final DBPDataSourceRegistry dsRegistry;
    private String objectTitle;
    private DBSObjectFilter filter;
    private boolean globalFilter;
    private Composite blockControl;
    private ControlEnableState blockEnableState;
    private Table includeTable;
    private Table excludeTable;
    private Combo namesCombo;
    private Button enableButton;

    public EditObjectFilterDialog(Shell shell, DBPDataSourceRegistry dsRegistry, String objectTitle, DBSObjectFilter filter, boolean globalFilter) {
        super(shell, IHelpContextIds.CTX_EDIT_OBJECT_FILTERS);
        this.dsRegistry = dsRegistry;
        this.objectTitle = objectTitle;
        this.filter = new DBSObjectFilter(filter);
        this.globalFilter = globalFilter;
    }

    public DBSObjectFilter getFilter() {
        return filter;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        getShell().setText(NLS.bind(UINavigatorMessages.dialog_filter_title, objectTitle));
        //getShell().setImage(DBIcon.EVENT.getImage());

        Composite composite = (Composite) super.createDialogArea(parent);

        Composite topPanel = UIUtils.createPlaceholder(composite, globalFilter ? 1 : 2);
        topPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        enableButton = UIUtils.createCheckbox(topPanel, UIMessages.button_enable, false);
        enableButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                filter.setEnabled(enableButton.getSelection());
                enableFiltersContent();
            }
        });
        enableButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        enableButton.setSelection(filter.isEnabled());
        if (!globalFilter) {
            Link globalLink = UIUtils.createLink(topPanel, UINavigatorMessages.dialog_filter_global_link, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    setReturnCode(SHOW_GLOBAL_FILTERS_ID);
                    close();
                }
            });
            globalLink.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        }
        blockControl = UIUtils.createPlaceholder(composite, 1);
        blockControl.setLayoutData(new GridData(GridData.FILL_BOTH));

        includeTable = StringEditorTable.createEditableList(blockControl, UINavigatorMessages.dialog_filter_list_include, filter.getInclude(), null, null);
        excludeTable = StringEditorTable.createEditableList(blockControl, UINavigatorMessages.dialog_filter_list_exclude, filter.getExclude(), null, null);

        UIUtils.createInfoLabel(blockControl, UINavigatorMessages.dialog_filter_hint_text);

        {
            Group sfGroup = UIUtils.createControlGroup(composite, UINavigatorMessages.dialog_filter_save_label, 4, GridData.FILL_HORIZONTAL, 0);
            namesCombo = UIUtils.createLabelCombo(sfGroup, UINavigatorMessages.dialog_filter_name_label, SWT.DROP_DOWN);
            namesCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            namesCombo.add(NULL_FILTER_NAME);
            List<String> sfNames = new ArrayList<>();
            for (DBSObjectFilter sf : dsRegistry.getSavedFilters()) {
                sfNames.add(sf.getName());
            }
            Collections.sort(sfNames);
            for (String sfName : sfNames) {
                namesCombo.add(sfName);
            }
            namesCombo.setText(CommonUtils.notEmpty(filter.getName()));
            namesCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    changeSavedFilter();
                }
            });

            Button saveButton = UIUtils.createPushButton(sfGroup, UINavigatorMessages.dialog_filter_save_button, null);
            saveButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    namesCombo.add(namesCombo.getText());
                    saveConfigurations();
                }
            });
            Button removeButton = UIUtils.createPushButton(sfGroup, UINavigatorMessages.dialog_filter_remove_button, null);
            removeButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    dsRegistry.removeSavedFilter(namesCombo.getText());
                    namesCombo.remove(namesCombo.getText());
                    namesCombo.setText(NULL_FILTER_NAME);
                }
            });
        }

        enableFiltersContent();

        return composite;
    }

    private void changeSavedFilter() {
        String filterName = namesCombo.getText();
        if (CommonUtils.equalObjects(filterName, filter.getName())) {
            return;
        }
        if (CommonUtils.isEmpty(filterName)) {
            // Reset filter
            StringEditorTable.fillFilterValues(includeTable, null, null);
            StringEditorTable.fillFilterValues(excludeTable, null, null);
        } else {
            // Find saved filter
            DBSObjectFilter savedFilter = dsRegistry.getSavedFilter(filterName);
            if (savedFilter != null) {
                StringEditorTable.fillFilterValues(includeTable, savedFilter.getInclude(), null);
                StringEditorTable.fillFilterValues(excludeTable, savedFilter.getExclude(), null);
            }
        }
        filter.setName(filterName);
    }

    private void enableFiltersContent() {
        if (filter.isEnabled()) {
            if (blockEnableState != null) {
                blockEnableState.restore();
                blockEnableState = null;
            }
        } else if (blockEnableState == null) {
            blockEnableState = ControlEnableState.disable(blockControl);
        }
    }

    private void saveConfigurations() {
        filter.setEnabled(enableButton.getSelection());
        filter.setInclude(StringEditorTable.collectValues(includeTable));
        filter.setExclude(StringEditorTable.collectValues(excludeTable));
        filter.setName(namesCombo.getText());
        if (!CommonUtils.isEmpty(filter.getName())) {
            dsRegistry.updateSavedFilter(filter);
        }
    }

    @Override
    protected void okPressed() {
        saveConfigurations();
        super.okPressed();
    }

    @Override
    protected void cancelPressed() {
        super.cancelPressed();
    }

}
