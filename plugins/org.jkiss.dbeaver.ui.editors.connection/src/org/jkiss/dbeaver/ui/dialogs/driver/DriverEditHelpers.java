/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.dialogs.StandardErrorDialog;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * DriverEditUtils
 */
public class DriverEditHelpers {
    private static final Log log = Log.getLog(DriverEditHelpers.class);

    static void exportDriverLibraries(Shell shell, List<DBPDriverLibrary> libraries) {
        String outputFolder = DialogUtils.openDirectoryDialog(
            shell,
            UIConnectionMessages.controls_client_homes_panel_label_path, null);
        if (CommonUtils.isEmpty(outputFolder)) {
            return;
        }
        Path[] firstExported = new Path[1];
        try {
            UIUtils.runInProgressDialog(monitor -> {
                int totalFiles = 0;
                monitor.beginTask("Count driver files", libraries.size());
                for (DBPDriverLibrary library : libraries) {
                    totalFiles += countFiles(monitor, library);
                    monitor.worked(1);
                }
                monitor.done();
                monitor.beginTask("Export driver files", totalFiles);
                for (DBPDriverLibrary library : libraries) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    try {
                        Path exported = exportLibrary(monitor, library, outputFolder);
                        if (exported == null) {
                            continue;
                        }
                        if (firstExported[0] == null) {
                            firstExported[0] = exported;
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                monitor.done();
            });
        } catch (InvocationTargetException e) {
            log.debug(e.getTargetException());
        }
        if (firstExported[0] != null) {
            ShellUtils.showInSystemExplorer(firstExported[0].toFile());
        }
    }

    private static int countFiles(DBRProgressMonitor monitor, DBPDriverLibrary library) {
        if (monitor.isCanceled()) {
            return 0;
        }
        int totalFiles = 0;
        try {
            Collection<? extends DBPDriverLibrary> dependencies = library.getDependencies(monitor);
            if (!CommonUtils.isEmpty(dependencies)) {
                for (DBPDriverLibrary dep : dependencies) {
                    totalFiles += countFiles(monitor, dep);
                }
            }
        } catch (IOException e) {
            // ignore
        }
        Path localFile = library.getLocalFile();
        if (localFile == null) {
            try {
                library.downloadLibraryFile(monitor, false, "Download file");
            } catch (Exception e) {
                log.debug(e);
            }
        }
        if (localFile != null && Files.exists(localFile)) {
            if (Files.isDirectory(localFile)) {
                try (Stream<Path> walk = Files.walk(localFile)) {
                    totalFiles += walk
                        .sorted(Comparator.reverseOrder())
                        .toList().size();
                } catch (IOException e) {
                    log.debug(e);
                }
            } else {
                totalFiles++;
            }
        }
        return totalFiles;
    }

    private static Path exportLibrary(DBRProgressMonitor monitor, DBPDriverLibrary library, String outputFolder) throws InterruptedException {
        if (monitor.isCanceled()) {
            return null;
        }

        while (true) {
            try {
                Path depExported = null;
                Collection<? extends DBPDriverLibrary> dependencies = library.getDependencies(monitor);
                if (!CommonUtils.isEmpty(dependencies)) {
                    for (DBPDriverLibrary dep : dependencies) {
                        Path result = exportLibrary(monitor, dep, outputFolder);
                        if (depExported == null) {
                            depExported = result;
                        }
                    }
                }
                Path localFile = library.getLocalFile();
                if (localFile == null || !Files.exists(localFile)) {
                    return depExported;
                }
                Path outFile = null;
                if (Files.isDirectory(localFile)) {
                    Path[] exported = new Path[1];
                    try (Stream<Path> walk = Files.walk(localFile)) {
                        walk.forEach(source -> {
                            if (Files.isDirectory(source)) {
                                return;
                            }
                            monitor.subTask("Export file '" + source + "'");
                            Path destination = Path.of(outputFolder, source.getFileName().toString());
                            try {
                                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                log.debug(e);
                            }
                            if (exported[0] == null) exported[0] = destination;
                        });
                    }
                    outFile = exported[0];
                } else {
                    String driverFileName = localFile.getFileName().toString();
                    monitor.subTask("Export library file '" + driverFileName + "'");
                    outFile = Path.of(outputFolder, driverFileName);

                    Files.copy(localFile, outFile, StandardCopyOption.REPLACE_EXISTING);
                }
                monitor.worked(1);
                return outFile;
            } catch (IOException e) {
                DBPPlatformUI.UserResponse resp = DBWorkbench.getPlatformUI().showErrorStopRetryIgnore(
                    "Export driver files", e, false);
                switch (resp) {
                    case RETRY:
                        continue;
                    case STOP:
                    case CANCEL:
                        throw new InterruptedException();
                }
            }
        }
    }

    static void showFileInExplorer(Path localFile) {
        if (localFile != null) {
            if (Files.isDirectory(localFile)) {
                ShellUtils.launchProgram(localFile.toAbsolutePath().toString());
            } else {
                ShellUtils.showInSystemExplorer(localFile.toAbsolutePath().toString());
            }
        }
    }

    static List<DBPDriver> getDriversByLibrary(DBPDriverLibrary oldLib) {
        List<DBPDriver> drivers = new ArrayList<>();
        for (DataSourceProviderDescriptor dspd : DataSourceProviderRegistry.getInstance().getDataSourceProviders()) {
            for (DBPDriver driver : dspd.getDrivers()) {
                for (DBPDriverLibrary library : driver.getDriverLibraries()) {
                    if (CommonUtils.equalObjects(library.getPath(), oldLib.getPath())) {
                        drivers.add(driver);
                    }
                }
            }
        }
        return drivers;
    }

    public static void showBadConfigDialog(final Shell shell, final String message, final DBException error) {
        //log.debug(message);
        Runnable runnable = () -> {
            DBPDataSource dataSource = error instanceof DBDatabaseException dbe ? dbe.getDataSource() : null;
            String title = NLS.bind(UIConnectionMessages.dialog_edit_driver_dialog_bad_configuration,
                dataSource == null ? "<unknown driver>" : dataSource.getContainer().getDriver().getName());
            new BadDriverConfigDialog(shell, title, message == null ? title : message, error).open();
        };
        UIUtils.syncExec(runnable);
    }

    static class BadDriverConfigDialog extends StandardErrorDialog {

        private final DBPDataSource dataSource;

        BadDriverConfigDialog(Shell shell, String title, String message, DBException error) {
            super(
                shell == null ? UIUtils.getActiveWorkbenchShell() : shell,
                title,
                message,
                RuntimeUtils.stripStack(GeneralUtils.makeExceptionStatus(error)),
                IStatus.ERROR);
            dataSource = error instanceof DBDatabaseException dbe ? dbe.getDataSource() : null;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            createButton(parent, IDialogConstants.RETRY_ID, "Open Driver &Configuration", true);
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, false);
            createDetailsButton(parent);
        }

        @Override
        protected void buttonPressed(int id) {
            if (id == IDialogConstants.RETRY_ID) {
                UIUtils.asyncExec(() -> {
                    DriverEditDialog dialog = new DriverEditDialog(
                        UIUtils.getActiveWorkbenchShell(),
                        dataSource.getContainer().getDriver());
                    dialog.open();
                });
                super.buttonPressed(IDialogConstants.OK_ID);
            }
            super.buttonPressed(id);
        }
    }
}
