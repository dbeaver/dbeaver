/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.ui.pages.database;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseMappingContainer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseMappingType;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferUtils;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.tools.transfer.ui.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.editors.object.struct.PropertyObjectEditPage;

import java.lang.reflect.InvocationTargetException;

/**
 *  Dialog with tabs to change target table properties and table columns mapping
 */
public class ConfigureMetadataStructureDialog extends BaseDialog {

    private DataTransferWizard wizard;
    private DatabaseConsumerSettings settings;
    private DatabaseMappingContainer mapping;

    public ConfigureMetadataStructureDialog(@NotNull DataTransferWizard wizard,
                                            @NotNull DatabaseConsumerSettings settings,
                                            @NotNull DatabaseMappingContainer mapping) {
        super(wizard.getShell(), "Configure metadata structure", null);
        this.wizard = wizard;
        this.settings = settings;
        this.mapping = mapping;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        composite.setLayoutData(gd);

        TabFolder configTabs = new TabFolder(composite, SWT.TOP | SWT.FLAT);
        configTabs.setLayoutData(new GridData(GridData.FILL_BOTH));

        TabItem columnsMappingTab = new TabItem(configTabs, SWT.NONE);
        columnsMappingTab.setText(DTUIMessages.columns_mapping_dialog_shell_text + mapping.getTargetName());
        ColumnsMappingDialog columnsMappingDialog = new ColumnsMappingDialog(wizard, settings, mapping);
        columnsMappingDialog.createControl(configTabs);
        columnsMappingTab.setData(columnsMappingDialog);
        Control pageControl = columnsMappingDialog.getControl();
        columnsMappingTab.setControl(pageControl);

        final DBSObjectContainer container = settings.getContainer();
        if (container != null) {
            TabItem tablePropertiesTab = new TabItem(configTabs, SWT.NONE);
            tablePropertiesTab.setText("Table properties");
            DBPDataSource dataSource = container.getDataSource();
            DBCExecutionContext executionContext = DBUtils.getDefaultContext(dataSource, true);
            final DBSObject[] tableObject = {null};
            if (mapping.getMappingType() != DatabaseMappingType.create && mapping.getMappingType() != DatabaseMappingType.recreate) {
                tableObject[0] = mapping.getTarget();
            } else {
                try {
                    wizard.getRunnableContext().run(true, true, monitor -> {
                        monitor.beginTask("Generate new table object", 1);
                        try {
                            tableObject[0] = DatabaseTransferUtils.generateStructTable(monitor, executionContext, container, mapping);
                        } catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                        monitor.done();
                    });
                } catch (InvocationTargetException e) {
                    DBWorkbench.getPlatformUI().showError(
                        DTUIMessages.database_consumer_page_mapping_title_target_table,
                        DTUIMessages.database_consumer_page_mapping_message_error_generating_target_table,
                        e);
                } catch (InterruptedException e) {
                   // do nothing
                }
            }
            if (tableObject[0] != null) {
                final PropertyObjectEditPage page = new PropertyObjectEditPage(null, tableObject[0]);
                page.createControl(configTabs);
                tablePropertiesTab.setData(page);
                Control tablePageControl = page.getControl();
                tablePropertiesTab.setControl(tablePageControl);
            } else {
                Composite compositeEmpty = new Composite(configTabs, SWT.NONE);
                final Group group = UIUtils.createControlGroup(compositeEmpty, "Message", 1, GridData.FILL_BOTH, 0);
                Text messageText = new Text(group, SWT.READ_ONLY | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
                messageText.setText("Can't load new table info");
                tablePropertiesTab.setControl(compositeEmpty);
            }
        }

        configTabs.setSelection(0);

        return composite;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }
}
