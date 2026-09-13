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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverDescriptor;
import org.jkiss.dbeaver.ext.cdata.ui.internal.CDataUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.DatabaseNativeAuthModelConfigurator;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.IOException;
import java.nio.file.Path;

public class CDataAuthModelConfigurator extends DatabaseNativeAuthModelConfigurator {

    private Button runButton;
    private boolean builderRunning;

    @Override
    public void createControl(
        @NotNull Composite parent,
        @NotNull DBAAuthModel<?> object,
        @NotNull Runnable propertyChangeListener
    ) {
        super.createControl(parent, object, propertyChangeListener);

        runButton = UIUtils.createDialogButton(
            parent,
            CDataUIMessages.auth_native_url_builder_run,
            SelectionListener.widgetSelectedAdapter(event -> runNativeUrlBuilder())
        );
        runButton.setEnabled(dataSource != null);
        runButton.setToolTipText(CDataUIMessages.auth_native_url_builder_description);
        runButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
    }

    @Override
    public void loadSettings(@NotNull DBPDataSourceContainer dataSource) {
        super.loadSettings(dataSource);
        setRunButtonEnabled(!builderRunning);
    }

    private void runNativeUrlBuilder() {
        if (dataSource == null || !(dataSource.getDriver() instanceof CDataDriverDescriptor driver)) {
            return;
        }
        builderRunning = true;
        setRunButtonEnabled(false);
        new AbstractJob(CDataUIMessages.auth_native_url_builder_job) {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                try {
                    Path driverJar = driver.resolveDriver(monitor).jarPath().toAbsolutePath();
                    new ProcessBuilder(GeneralUtils.findJavaExecutable(), "-jar", driverJar.toString())
                        .directory(driverJar.getParent().toFile())
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start()
                        .onExit()
                        .thenAccept(process -> {
                            UIUtils.asyncExec(() -> {
                                builderRunning = false;
                                if (process.exitValue() != 0) {
                                    DBWorkbench.getPlatformUI().showError(
                                        CDataUIMessages.auth_native_url_builder_error,
                                        NLS.bind(CDataUIMessages.auth_native_url_builder_exit_code, process.exitValue())
                                    );
                                }
                                setRunButtonEnabled(true);
                            });
                        });
                    return Status.OK_STATUS;
                } catch (DBException | IOException e) {
                    UIUtils.asyncExec(() -> {
                        builderRunning = false;
                        setRunButtonEnabled(true);
                        DBWorkbench.getPlatformUI().showError(
                            CDataUIMessages.auth_native_url_builder_error,
                            CDataUIMessages.auth_native_url_builder_error,
                            e
                        );
                    });
                    return Status.error(CDataUIMessages.auth_native_url_builder_error, e);
                }
            }
        }.schedule();
    }

    private void setRunButtonEnabled(boolean enabled) {
        if (runButton != null && !runButton.isDisposed()) {
            runButton.setEnabled(enabled);
        }
    }
}
