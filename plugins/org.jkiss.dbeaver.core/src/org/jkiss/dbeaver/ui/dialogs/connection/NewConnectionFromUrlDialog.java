/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCURL;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.stream.Collectors;

public class NewConnectionFromUrlDialog extends BaseDialog {
    private static final int INPUT_DELAY_BEFORE_REFRESH = 300;

    private TreeViewer driverViewer;
    private CLabel errorLabel;

    private String url;
    private DBPDriver driver;

    public NewConnectionFromUrlDialog(@NotNull Shell shell) {
        super(shell, CoreMessages.dialog_connection_from_url_title, null);
        setShellStyle(SWT.TITLE | SWT.CLOSE | SWT.RESIZE | SWT.BORDER);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        final Composite parentComposite = super.createDialogArea(parent);

        {
            final Composite composite = UIUtils.createComposite(parentComposite, 1);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));
            ((GridData) composite.getLayoutData()).widthHint = 500;

            final Text urlText = UIUtils.createLabelText(composite, CoreMessages.dialog_connection_from_url_url, null);
            urlText.setMessage("jdbc:postgresql://localhost:5432/dbeaver");

            UIUtils.createControlLabel(composite, CoreMessages.dialog_connection_from_url_drivers);
            driverViewer = new TreeViewer(composite, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
            driverViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
            driverViewer.setContentProvider(new DriverContentProvider());
            driverViewer.setLabelProvider(new DriverLabelProvider());
            driverViewer.addSelectionChangedListener(event -> {
                final Object element = event.getStructuredSelection().getFirstElement();
                if (element instanceof DBPDriver) {
                    driver = (DBPDriver) element;
                }
                updateCompletion();
            });

            errorLabel = new CLabel(composite, SWT.NONE);
            errorLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            errorLabel.setImage(JFaceResources.getImage(DLG_IMG_MESSAGE_ERROR));
            errorLabel.setVisible(false);

            final AbstractJob refreshJob = new AbstractJob("Refresh suitable drivers timeout") {
                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    UIUtils.asyncExec(() -> {
                        final List<Map.Entry<DBPDataSourceProviderDescriptor, List<DBPDriver>>> drivers = getSuitableDrivers(urlText.getText());

                        url = urlText.getText();
                        driverViewer.getTree().setRedraw(false);
                        driverViewer.setInput(drivers);
                        driverViewer.expandAll();
                        driverViewer.getTree().setRedraw(true);

                        if (!drivers.isEmpty()) {
                            driverViewer.setSelection(new StructuredSelection(drivers.get(0).getValue().get(0)));
                        }

                        updateCompletion();
                    });
                    return Status.OK_STATUS;
                }
            };
            refreshJob.setSystem(true);
            refreshJob.setUser(false);

            urlText.addModifyListener(event -> {
                if (!refreshJob.isCanceled()) {
                    refreshJob.cancel();
                }
                refreshJob.schedule(INPUT_DELAY_BEFORE_REFRESH);
            });
            urlText.addDisposeListener(event -> {
                if (!refreshJob.isCanceled()) {
                    refreshJob.cancel();
                }
            });
        }

        return parentComposite;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.PROCEED_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        updateCompletion();
    }

    @NotNull
    public DBPDriver getDriver() {
        return driver;
    }

    @NotNull
    public String getUrl() {
        return url;
    }

    @Nullable
    public DBPConnectionConfiguration extractConnectionConfiguration() {
        final String sampleUrl = Objects.requireNonNull(driver.getSampleURL());
        return JDBCURL.extractConfigurationFromUrl(sampleUrl, url);
    }

    @NotNull
    private List<Map.Entry<DBPDataSourceProviderDescriptor, List<DBPDriver>>> getSuitableDrivers(@NotNull String url) {
        final Map<DBPDataSourceProviderDescriptor, List<DBPDriver>> result = new LinkedHashMap<>();
        final Map<DBPDataSourceProviderDescriptor, Integer> scores = new HashMap<>();

        for (DBPDataSourceProviderDescriptor provider : DataSourceProviderRegistry.getInstance().getDataSourceProviders()) {
            final List<DBPDriver> drivers = new ArrayList<>();

            for (DBPDriver driver : provider.getEnabledDrivers()) {
                if (CommonUtils.isEmpty(driver.getSampleURL())) {
                    continue;
                }
                if (JDBCURL.getPattern(driver.getSampleURL()).matcher(url).matches()) {
                    drivers.add(driver);
                    scores.put(provider, scores.computeIfAbsent(provider, x -> 0) + 1);
                }
            }

            if (!drivers.isEmpty()) {
                drivers.sort(Comparator
                    .comparing(DBPDriver::getPromotedScore)
                    .thenComparing(DBPDriver::getFullName)
                    .reversed());

                result.put(provider, drivers);
            }
        }

        return result.entrySet().stream()
            .sorted(Comparator.comparing(x -> scores.get(x.getKey()), Comparator.reverseOrder()))
            .collect(Collectors.toList());
    }

    private void updateCompletion() {
        if (driverViewer.getTree().getItemCount() == 0) {
            setCompleted(false, CoreMessages.dialog_connection_from_url_error_no_drivers_found);
            return;
        }
        if (!(driverViewer.getStructuredSelection().getFirstElement() instanceof DBPDriver)) {
            setCompleted(false, CoreMessages.dialog_connection_from_url_error_no_driver_selected);
            return;
        }
        setCompleted(true, "");
    }

    private void setCompleted(boolean valid, @NotNull String message) {
        getButton(IDialogConstants.OK_ID).setEnabled(valid);
        errorLabel.setVisible(!valid && CommonUtils.isNotEmpty(message));
        errorLabel.setText(message);
    }

    private static class DriverLabelProvider extends LabelProvider {
        @Override
        @SuppressWarnings("unchecked")
        public String getText(Object element) {
            if (element instanceof Map.Entry) {
                final Map.Entry<DBPDataSourceProviderDescriptor, List<DBPDriver>> entry = (Map.Entry<DBPDataSourceProviderDescriptor, List<DBPDriver>>) element;
                return entry.getKey().getName();
            }
            if (element instanceof DBPDriver) {
                return ((DBPDriver) element).getName();
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Image getImage(Object element) {
            if (element instanceof Map.Entry) {
                final Map.Entry<DBPDataSourceProviderDescriptor, List<DBPDriver>> entry = (Map.Entry<DBPDataSourceProviderDescriptor, List<DBPDriver>>) element;
                return DBeaverIcons.getImage(entry.getKey().getIcon());
            }
            if (element instanceof DBPDriver) {
                return DBeaverIcons.getImage(((DBPDriver) element).getIcon());
            }
            return null;
        }
    }

    private static class DriverContentProvider extends TreeContentProvider {
        @Override
        @SuppressWarnings("unchecked")
        public Object[] getChildren(Object element) {
            if (element instanceof Map.Entry) {
                final Map.Entry<DBPDataSourceProviderDescriptor, List<DBPDriver>> entry = (Map.Entry<DBPDataSourceProviderDescriptor, List<DBPDriver>>) element;
                return entry.getValue().toArray();
            }
            return null;
        }

        @Override
        public boolean hasChildren(Object element) {
            return element instanceof Map.Entry;
        }
    }
}
