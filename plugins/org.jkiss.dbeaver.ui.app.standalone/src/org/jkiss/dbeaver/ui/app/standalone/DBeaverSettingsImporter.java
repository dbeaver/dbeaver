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
package org.jkiss.dbeaver.ui.app.standalone;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.runtime.BaseProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.StandardConstants;

import java.io.*;
import java.util.Properties;

/**
 * This class controls all aspects of the application's execution
 */
class DBeaverSettingsImporter {

    private static final String[] COPY_PLUGINS = {
        "org.eclipse.compare",
        "org.eclipse.core.resources",
        "org.eclipse.core.runtime",
        "org.eclipse.e4.ui.workbench.swt",
        "org.eclipse.equinox.p2.ui",
        "org.eclipse.equinox.security.ui",
        "org.eclipse.help.ui",
        "org.eclipse.search",
        "org.eclipse.ui.ide",
        "org.eclipse.ui.views.log",
        "org.eclipse.ui.workbench",
        "org.eclipse.ui.workbench.texteditor",
    };

    private final DBeaverApplication application;
    private final Display display;

    private Shell windowShell;
    private Label progressLabel;
    private ProgressBar progressBar;
    private File oldDriversFolder;
    private File driversFolder;
    private File oldWorkspacePath;

    private int shellResult = SWT.NONE;

    public DBeaverSettingsImporter(DBeaverApplication application, Display display) {
        this.application = application;
        this.display = display;
    }

    boolean migrateFromPreviousVersion(final File oldDir, final File newDir) {

        Properties workspaceProps = new Properties();
        File versionFile = new File(GeneralUtils.getMetadataFolder(oldDir), DBConstants.WORKSPACE_PROPS_FILE);
        if (versionFile.exists()) {
            try (InputStream is = new FileInputStream(versionFile)) {
                workspaceProps.load(is);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }

        String oldVersion = workspaceProps.getProperty(DBeaverApplication.VERSION_PROP_PRODUCT_VERSION);
        if (oldVersion == null) {
            oldVersion = "3.x";
        } else {
            oldVersion = GeneralUtils.getPlainVersion(oldVersion);
        }
        oldWorkspacePath = oldDir;
        oldDriversFolder = new File(
            System.getProperty(StandardConstants.ENV_USER_HOME),
            DBConstants.LEGACY_DRIVERS_FOLDER);
        driversFolder = new File(
            newDir.getParent(),
            //System.getProperty(StandardConstants.ENV_USER_HOME),
            DBConstants.DEFAULT_DRIVERS_FOLDER);

        Image dbeaverIcon = AbstractUIPlugin.imageDescriptorFromPlugin(DBeaverApplication.APPLICATION_PLUGIN_ID, "icons/dbeaver32.png").createImage();
        Image dbeaverLogo = AbstractUIPlugin.imageDescriptorFromPlugin(DBeaverApplication.APPLICATION_PLUGIN_ID, "icons/dbeaver64.png").createImage();

        // Hide splash if any
        WorkbenchPlugin.unsetSplashShell(display);

        // Make new shell
        windowShell = new Shell(display);
        windowShell.setImage(dbeaverIcon);
        windowShell.setText("Import " + GeneralUtils.getProductName() + " configuration");
        windowShell.setLayout(new GridLayout(1, false));

        {
            Group infoGroup = new Group(windowShell, SWT.NONE);
            infoGroup.setText("Import workspace");
            infoGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
            GridLayout gl = new GridLayout(2, false);
            gl.horizontalSpacing = 10;
            infoGroup.setLayout(gl);
            Label iconLabel = new Label(infoGroup, SWT.NONE);
            iconLabel.setImage(dbeaverLogo);
            iconLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            Label confirmLabel = new Label(infoGroup, SWT.NONE);
            //confirmLabel.setImage(JFaceResources.getImage(org.eclipse.jface.dialogs.Dialog.DLG_IMG_MESSAGE_INFO));
            confirmLabel.setText(
                "\n" +
                GeneralUtils.getProductTitle() + " uses a new configuration format.\n\n" +
//                "Previous version (" + GeneralUtils.getProductName() + " " + oldVersion + ") settings were found.\n" +
//                oldDir.getAbsolutePath() + "\n" +
                "Do you want to migrate existing settings (version " + oldVersion + ")?\n\n"
//                "Make sure previous version of " + GeneralUtils.getProductName() + " isn't running"
            );
            confirmLabel.setLayoutData(new GridData(GridData.FILL_BOTH));
        }

        {
            Composite buttonsPanel = new Composite(windowShell, SWT.NONE);
            buttonsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            buttonsPanel.setLayout(new GridLayout(2, true));
            final Button migrateButton = new Button(buttonsPanel, SWT.PUSH);
            migrateButton.setText("Migrate (Recommended)");

            final Button skipButton = new Button(buttonsPanel, SWT.PUSH);
            skipButton.setText("Do not migrate");

            migrateButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            migrateButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    migrateButton.setEnabled(false);
                    skipButton.setEnabled(false);
                    progressBar.setVisible(true);
                    ((GridData)progressBar.getLayoutData()).exclude = false;
                    windowShell.pack();
                    migrateWorkspace(oldDir, newDir);
                }
            });
            windowShell.setDefaultButton(migrateButton);

            skipButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            skipButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    MessageBox messageBox = new MessageBox(windowShell, SWT.ICON_WARNING | SWT.YES | SWT.NO);
                    messageBox.setText("Skip workspace migration");
                    messageBox.setMessage("You will lose all previous configurations and scripts.\n\nAre you sure?");
                    int response = messageBox.open();
                    if (response == SWT.YES) {
                        shellResult = SWT.IGNORE;
                        windowShell.dispose();
                    }
                }
            });
        }

        progressLabel = new Label(windowShell, SWT.NONE);
        progressLabel.setLayoutData(new GridData(GridData.FILL_BOTH));
        progressBar = new ProgressBar(windowShell, SWT.SMOOTH);
        progressBar.setVisible(false);
        final GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 30;
        gd.widthHint = 300;
        gd.exclude = true;
        progressBar.setLayoutData(gd);
        windowShell.pack();

        Rectangle screenSize = display.getPrimaryMonitor().getBounds();
        windowShell.setLocation((screenSize.width - windowShell.getBounds().width) / 2, (screenSize.height - windowShell.getBounds().height) / 2);

        windowShell.open();

        while (!windowShell.isDisposed ()) {
            if (!display.readAndDispatch ()) display.sleep ();
        }

        return (shellResult != SWT.NONE);
    }

    private void migrateWorkspace(final File oldDir, final File newDir) {
        progressLabel.setText("Counting workspace files...");
        long totalFiles = countWorkspaceFiles(oldDir);
        totalFiles += countWorkspaceFiles(oldDriversFolder);
        progressBar.setMinimum(0);
        progressBar.setMaximum((int) (totalFiles / 1000));

        final DBRProgressMonitor monitor = new BaseProgressMonitor() {
            long bytesProcessed = 0;
            @Override
            public void subTask(final String name) {
                display.syncExec(() -> progressLabel.setText(name));
            }
            @Override
            public void worked(final int work) {
                display.syncExec(() -> {
                    bytesProcessed += work;
                    progressBar.setSelection((int) (bytesProcessed / 1000));
                });
            }
        };
        new Thread(() -> {
            try {
                if (!newDir.exists()) {
                    if (!newDir.mkdirs()) {
                        System.err.println("Can't create target workspace directory '" + newDir.getAbsolutePath() + "'");
                        return;
                    }
                }
                copyWorkspaceFiles(monitor, DIR_TYPE.WORKSPACE, oldDir, newDir);
                copyFolderFiles(monitor, oldDriversFolder, driversFolder);
            } finally {
                DBeaverApplication.WORKSPACE_MIGRATED = true;
                display.syncExec(new Runnable() {
                    @Override
                    public void run() {
                        showMessageBox("Import completed", "Configuration was imported to '" + newDir.getAbsolutePath() + "'", SWT.ICON_INFORMATION | SWT.OK);
                        shellResult = SWT.OK;
                        windowShell.dispose();
                    }
                });
            }
        }).start();
    }

    private int countWorkspaceFiles(File dir) {
        int count = 1;
        final File[] files = dir.listFiles();
        if (files == null) {
            return count;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                count += countWorkspaceFiles(file);
            } else {
                count += file.length();
            }
        }
        return count;
    }

    enum DIR_TYPE {
        WORKSPACE,
        NORMAL,
        METADATA,
        PLUGINS,
        CORE
    }

    private void copyWorkspaceFiles(DBRProgressMonitor monitor, DIR_TYPE parentDirType, File fromDir, File toDir) {
        final File[] files = fromDir.listFiles();
        if (files == null) {
            return;
        }

        long skippedFiles = 0;
        for (File file : files) {

            DIR_TYPE dirType = DIR_TYPE.NORMAL;
            String fileName = file.getName();
            switch (fileName) {
                case ".metadata":
                    if (parentDirType == DIR_TYPE.WORKSPACE) {
                        dirType = DIR_TYPE.METADATA;
                    }
                    break;
                case ".plugins":
                    if (parentDirType == DIR_TYPE.METADATA) {
                        dirType = DIR_TYPE.PLUGINS;
                    }
                    break;
                case "org.jkiss.dbeaver.core":
                    if (parentDirType == DIR_TYPE.PLUGINS) {
                        dirType = DIR_TYPE.CORE;
                    }
                    break;
            }

            String relPath = file.getAbsolutePath().substring(oldWorkspacePath.getAbsolutePath().length());
            monitor.subTask(relPath);
            if (file.isDirectory()) {
                if (parentDirType == DIR_TYPE.METADATA && !(fileName.equals(".plugins") || fileName.equals("qmdb") || fileName.startsWith("dbeaver"))) {
                    // Skip all dirs but plugins
                    skippedFiles += countWorkspaceFiles(file);
                    continue;
                }
                if (parentDirType == DIR_TYPE.PLUGINS) {
                    if (!fileName.contains("dbeaver") && !ArrayUtils.contains(COPY_PLUGINS, fileName)) {
                        skippedFiles += countWorkspaceFiles(file);
                        continue;
                    }
                }
                if (parentDirType == DIR_TYPE.CORE) {
                    // Copy drivers in the separate location
                    switch (fileName) {
                        case "drivers":
                        case "maven":
                        case "remote":
                            File targetDir = new File(driversFolder, fileName);
                            if (!targetDir.exists()) {
                                if (!targetDir.mkdirs()) {
                                    System.err.println("Can't create drivers folder " + targetDir.getAbsolutePath());
                                    skippedFiles += countWorkspaceFiles(file);
                                    continue;
                                }
                            }
                            copyWorkspaceFiles(monitor, DIR_TYPE.NORMAL, file, targetDir);
                            continue;
                    }
                }
                File newDir = new File(toDir, fileName);
                if (newDir.exists() || newDir.mkdir()) {
                    copyWorkspaceFiles(monitor, dirType, file, newDir);
                } else {
                    System.err.println("Can't create folder " + newDir.getAbsolutePath());
                }
            } else {
                if (parentDirType == DIR_TYPE.METADATA && fileName.startsWith(".")) {
                    // Skip ALL hidden files in .metadata
                    skippedFiles++;
                    continue;
                }
                File newFile = new File(toDir, fileName);
                if (fileName.equals("org.jkiss.dbeaver.core.prefs")) {
                    // Patch configuration
                    Properties coreProps = new Properties();
                    try (FileInputStream is = new FileInputStream(file)) {
                        coreProps.load(is);
                    } catch (IOException e) {
                        e.printStackTrace(System.err);
                    }
                    final String driversHomeProp = coreProps.getProperty("ui.drivers.home");
                    if (driversHomeProp != null && !driversHomeProp.isEmpty()) {
                        File oldDriversPath = new File(driversHomeProp);
                        if (oldDriversPath.equals(new File(oldWorkspacePath, ".metadata/.plugins/org.jkiss.dbeaver.core"))) {
                            // Set new drivers path
                            coreProps.setProperty("ui.drivers.home", driversFolder.getAbsolutePath());
                        }
                    }
                    try (FileOutputStream os = new FileOutputStream(newFile)) {
                        coreProps.store(os, null);
                    } catch (IOException e) {
                        e.printStackTrace(System.err);
                    }
                } else if (parentDirType == DIR_TYPE.CORE && fileName.equals("drivers.xml")) {
                    // Patch drivers cache path
                    String driversText = null;
                    try (Reader r = new InputStreamReader(new FileInputStream(file), GeneralUtils.UTF8_CHARSET)) {
                        driversText = IOUtils.readToString(r);
                        driversText = driversText.replace("${workspace}\\.metadata\\.plugins\\org.jkiss.dbeaver.core", "${drivers_home}");
                        driversText = driversText.replace("${workspace}/.metadata/.plugins/org.jkiss.dbeaver.core", "${drivers_home}");
                    } catch (IOException e) {
                        e.printStackTrace(System.err);
                    }
                    if (driversText != null) {
                        try (Writer w = new OutputStreamWriter(new FileOutputStream(newFile), GeneralUtils.UTF8_CHARSET)) {
                            w.write(driversText);
                        } catch (IOException e) {
                            e.printStackTrace(System.err);
                        }
                    }
                } else {
                    copyFileContents(file, newFile);

                }
            }
            monitor.worked((int) (file.length() + skippedFiles));
        }
    }

    private void copyFolderFiles(DBRProgressMonitor monitor, File fromDir, File toDir) {
        final File[] files = fromDir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            monitor.subTask(file.getName());
            if (file.isDirectory()) {
                File newDir = new File(toDir, file.getName());
                if (newDir.exists() || newDir.mkdirs()) {
                    copyFolderFiles(monitor, file, newDir);
                } else {
                    System.err.println("Can't create folder " + newDir.getAbsolutePath());
                }
            } else {
                copyFileContents(file, new File(toDir, file.getName()));
                monitor.worked((int) file.length());
            }
        }
    }

    private void copyFileContents(File file, File newFile) {
        // Just copy the file
        if (newFile.exists() && newFile.length() == file.length()) {
            // It is already there
        } else {
            try (FileInputStream is = new FileInputStream(file)) {
                try (FileOutputStream os = new FileOutputStream(newFile)) {
                    IOUtils.fastCopy(is, os, 100000);
                }
            } catch (IOException e) {
                // Error
                e.printStackTrace(System.err);
            }
        }
    }

    private int showMessageBox(String title, String message, int style) {
        // Can't lock specified path
        MessageBox messageBox = new MessageBox(windowShell, style);
        messageBox.setText(title);
        messageBox.setMessage(message);
        return messageBox.open();
    }

}
