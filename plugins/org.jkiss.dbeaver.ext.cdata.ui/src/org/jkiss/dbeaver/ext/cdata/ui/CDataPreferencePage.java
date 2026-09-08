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
package org.jkiss.dbeaver.ext.cdata.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.cdata.CDataLicenseUIService;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverDescriptor;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverLicense;
import org.jkiss.dbeaver.ext.cdata.registry.CDataLicenseStatus;
import org.jkiss.dbeaver.ext.cdata.registry.CDataLicenseType;
import org.jkiss.dbeaver.ext.cdata.registry.CDataPersistedLicense;
import org.jkiss.dbeaver.ext.cdata.registry.CDataResolvedDriver;
import org.jkiss.dbeaver.ext.cdata.ui.internal.CDataUIMessages;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CDataPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage {
    private static final Log log = Log.getLog(CDataPreferencePage.class);
    private static final String PROVIDER_ID = "cdata";

    private TableViewer licenseViewer;
    private Label statusLabel;
    private Button addKeyButton;
    private Button refreshButton;
    private AbstractJob refreshJob;
    private volatile int refreshGeneration;

    public CDataPreferencePage() {
        noDefaultAndApplyButton();
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 1);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Label description = UIUtils.createLabel(composite, CDataUIMessages.preference_description);
        description.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite tableComposite = UIUtils.createComposite(composite, 1);
        tableComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        licenseViewer = new TableViewer(tableComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
        licenseViewer.setContentProvider(ArrayContentProvider.getInstance());
        Table table = licenseViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData tableData = new GridData(GridData.FILL_HORIZONTAL);
        tableData.heightHint = table.getItemHeight() * 8;
        table.setLayoutData(tableData);
        createColumns();
        table.addListener(SWT.Resize, event -> resizeColumns());

        Composite buttons = new Composite(tableComposite, SWT.NONE);
        GridLayout buttonLayout = new GridLayout(2, false);
        buttonLayout.marginWidth = 0;
        buttons.setLayout(buttonLayout);
        buttons.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
        addKeyButton = UIUtils.createDialogButton(
            buttons,
            CDataUIMessages.preference_add_key,
            SelectionListener.widgetSelectedAdapter(event -> addLicenseKey())
        );
        licenseViewer.addSelectionChangedListener(event -> updateActionState());
        refreshButton = UIUtils.createDialogButton(
            buttons,
            CDataUIMessages.preference_refresh,
            SelectionListener.widgetSelectedAdapter(event -> refreshLicenses())
        );

        statusLabel = UIUtils.createLabel(composite, "");
        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        refreshLicenses();
        return composite;
    }

    @Override
    public void dispose() {
        refreshGeneration++;
        if (refreshJob != null) {
            refreshJob.cancel();
        }
        super.dispose();
    }

    private void createColumns() {
        createColumn(CDataUIMessages.preference_column_driver, 180, entry -> entry.driver().getName());
        createColumn(CDataUIMessages.preference_column_version, 90, LicenseEntry::majorVersion);
        createColumn(CDataUIMessages.preference_column_type, 80, entry -> getLicenseType(entry.license()));
        createColumn(
            CDataUIMessages.preference_column_status,
            140,
            entry -> CDataLicenseUIUtils.getStatusText(entry.license().getStatus())
        );
        createColumn(CDataUIMessages.preference_column_expiration, 120, entry -> getExpiration(entry.license()));
    }

    private void resizeColumns() {
        Table table = licenseViewer.getTable();
        int width = table.getClientArea().width;
        if (width <= 0) {
            return;
        }
        int[] weights = {30, 14, 14, 24, 18};
        int used = 0;
        for (int i = 0; i < table.getColumnCount() - 1; i++) {
            int columnWidth = width * weights[i] / 100;
            table.getColumn(i).setWidth(columnWidth);
            used += columnWidth;
        }
        table.getColumn(table.getColumnCount() - 1).setWidth(Math.max(0, width - used));
    }

    private void createColumn(@NotNull String title, int width, @NotNull LicenseTextProvider textProvider) {
        TableViewerColumn column = new TableViewerColumn(licenseViewer, SWT.LEFT);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);
        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return textProvider.getText((LicenseEntry) element);
            }

            @Override
            public Image getImage(Object element) {
                if (licenseViewer.getTable().indexOf(column.getColumn()) == 0) {
                    return DBeaverIcons.getImage(((LicenseEntry) element).driver().getIcon());
                }
                return null;
            }
        });
    }

    private void refreshLicenses() {
        int generation = ++refreshGeneration;
        setLoading(true);
        refreshJob = new AbstractJob(CDataUIMessages.preference_load_job) {
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                List<LicenseEntry> licenses = new ArrayList<>();
                List<CDataDriverDescriptor> drivers = getDrivers();
                monitor.beginTask(CDataUIMessages.preference_loading, drivers.size());
                try {
                    for (CDataDriverDescriptor driver : drivers) {
                        if (monitor.isCanceled()) {
                            return Status.CANCEL_STATUS;
                        }
                        loadLicense(monitor, driver, licenses);
                        monitor.worked(1);
                    }
                } finally {
                    monitor.done();
                }
                UIUtils.asyncExec(() -> showLicenses(generation, licenses));
                return Status.OK_STATUS;
            }
        };
        refreshJob.setSystem(true);
        refreshJob.schedule();
    }

    private static void loadLicense(
        @NotNull DBRProgressMonitor monitor,
        @NotNull CDataDriverDescriptor driver,
        @NotNull List<LicenseEntry> licenses
    ) {
        try {
            for (CDataPersistedLicense persistedLicense : driver.inspectPersistedLicenses(monitor)) {
                licenses.add(new LicenseEntry(
                    driver,
                    persistedLicense.majorVersion(),
                    persistedLicense.license(),
                    persistedLicense.resolvedDriver()
                ));
            }
        } catch (DBException e) {
            log.debug("Unable to read the CData license for '" + driver.getName() + "'", e);
            licenses.add(new LicenseEntry(
                driver,
                "",
                new CDataDriverLicense(CDataLicenseStatus.VALIDATION_UNAVAILABLE, "", e.getMessage()),
                null
            ));
        }
    }

    private void showLicenses(int generation, @NotNull List<LicenseEntry> licenses) {
        if (generation != refreshGeneration || licenseViewer.getControl().isDisposed()) {
            return;
        }
        licenseViewer.setInput(licenses);
        licenses.stream()
            .map(LicenseEntry::driver)
            .distinct()
            .forEach(driver -> driver.loadIcon(this::redrawLicenseIcons));
        statusLabel.setText(licenses.isEmpty()
            ? CDataUIMessages.preference_no_licenses
            : NLS.bind(CDataUIMessages.preference_license_count, licenses.size()));
        setLoading(false);
    }

    private void redrawLicenseIcons() {
        UIUtils.asyncExec(() -> {
            if (licenseViewer != null && !licenseViewer.getControl().isDisposed()) {
                licenseViewer.refresh();
            }
        });
    }

    private void setLoading(boolean loading) {
        if (licenseViewer == null || licenseViewer.getControl().isDisposed()) {
            return;
        }
        refreshButton.setEnabled(!loading);
        updateActionState();
        if (loading) {
            statusLabel.setText(CDataUIMessages.preference_loading);
        }
    }

    private void updateActionState() {
        if (addKeyButton != null && !addKeyButton.isDisposed()) {
            addKeyButton.setEnabled(
                refreshButton != null && refreshButton.isEnabled() && getSelectedTrial() != null
            );
        }
    }

    @Nullable
    private LicenseEntry getSelectedTrial() {
        Object selected = licenseViewer.getStructuredSelection().getFirstElement();
        if (selected instanceof LicenseEntry entry &&
            entry.license().isTrialLicense() &&
            entry.activationTarget() != null
        ) {
            return entry;
        }
        return null;
    }

    private void addLicenseKey() {
        LicenseEntry entry = getSelectedTrial();
        if (entry == null) {
            return;
        }
        CDataLicenseUIService service = DBWorkbench.getService(CDataLicenseUIService.class);
        if (service == null) {
            DBWorkbench.getPlatformUI().showError(
                CDataUIMessages.activation_failed,
                CDataUIMessages.preference_service_unavailable
            );
            return;
        }
        try {
            if (service.activateLicense(
                entry.driver(),
                CDataLicenseType.PURCHASED,
                entry.activationTarget()
            ) != null) {
                refreshLicenses();
            }
        } catch (UnsupportedOperationException e) {
            DBWorkbench.getPlatformUI().showError(
                CDataUIMessages.activation_failed,
                CDataUIMessages.preference_service_unavailable
            );
        }
    }

    @NotNull
    private static List<CDataDriverDescriptor> getDrivers() {
        DataSourceProviderDescriptor provider = DataSourceProviderRegistry.getInstance().getDataSourceProvider(PROVIDER_ID);
        if (provider == null) {
            return List.of();
        }
        return provider.getEnabledDrivers().stream()
            .filter(CDataDriverDescriptor.class::isInstance)
            .map(CDataDriverDescriptor.class::cast)
            .sorted(Comparator.comparing(CDataDriverDescriptor::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @NotNull
    private static String getLicenseType(@NotNull CDataDriverLicense license) {
        if (license.isTrialLicense()) {
            return CDataUIMessages.preference_type_trial;
        }
        return switch (license.getStatus()) {
            case PURCHASED_ACTIVE, PURCHASED_EXPIRING, EXPIRED -> CDataUIMessages.preference_type_purchased;
            default -> CDataUIMessages.preference_type_unknown;
        };
    }

    @NotNull
    private static String getExpiration(@NotNull CDataDriverLicense license) {
        if (license.getRemainingDays() != null) {
            return NLS.bind(CDataUIMessages.preference_expiration_days, license.getRemainingDays());
        }
        if (license.getStatus() == CDataLicenseStatus.TRIAL_EXPIRED ||
            license.getStatus() == CDataLicenseStatus.EXPIRED) {
            return CDataUIMessages.preference_expiration_expired;
        }
        return CDataUIMessages.preference_expiration_unavailable;
    }

    private record LicenseEntry(
        @NotNull CDataDriverDescriptor driver,
        @NotNull String majorVersion,
        @NotNull CDataDriverLicense license,
        @Nullable CDataResolvedDriver activationTarget
    ) {
    }

    @FunctionalInterface
    private interface LicenseTextProvider {
        @NotNull
        String getText(@NotNull LicenseEntry entry);
    }
}
