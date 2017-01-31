/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.core.application;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
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
        "org.jkiss.dbeaver.core",
        "org.jkiss.dbeaver.model",
    };

    private final DBeaverApplication application;
    private final Display display;

    private Shell progressShell;
    private Label progressLabel;
    private ProgressBar progressBar;
    private File driversFolder;
    private File oldWorkspacePath;

    public DBeaverSettingsImporter(DBeaverApplication application, Display display) {
        this.application = application;
        this.display = display;
    }

    void migrateFromPreviousVersion(final File oldDir, final File newDir) {
        final Properties oldProps = application.readWorkspaceInfo(GeneralUtils.getMetadataFolder(oldDir));
        String oldVersion = oldProps.getProperty(DBeaverApplication.VERSION_PROP_PRODUCT_VERSION);
        if (oldVersion == null) {
            oldVersion = "3.x";
        }
        oldWorkspacePath = oldDir;
        driversFolder = new File(
            System.getProperty(StandardConstants.ENV_USER_HOME),
            DBConstants.DEFAULT_DRIVERS_FOLDER);
        int msgResult = application.showMessageBox(
            "DBeaver - Import settings",
            "Settings of previous version (" + oldVersion + ") of " + GeneralUtils.getProductName() + " was found at\n" +
                oldDir.getAbsolutePath() + "\n" +
                "Do you want to import previous version settings? (Recommended).\n\n" +
                "Make sure previous version of " + GeneralUtils.getProductName() + " isn't running",
            SWT.ICON_QUESTION | SWT.NO | SWT.YES);
        if (msgResult != SWT.YES) {
            return;
        }
        if (!newDir.exists()) {
            if (!newDir.mkdirs()) {
                application.showMessageBox("Error", "Can't create workspace folder '" + newDir.getAbsolutePath() + "'", SWT.ICON_ERROR | SWT.OK);
                return;
            }
        }

        progressShell = new Shell(display);
        progressShell.setText("Import " + GeneralUtils.getProductName() + " configuration");
        progressShell.setLayout(new GridLayout(1, false));
        progressLabel = new Label(progressShell, SWT.NONE);
        progressLabel.setLayoutData(new GridData(GridData.FILL_BOTH));
        progressBar = new ProgressBar(progressShell, SWT.SMOOTH);
        final GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 30;
        gd.widthHint = 300;
        progressBar.setLayoutData(gd);
        progressShell.pack();

        Rectangle screenSize = display.getPrimaryMonitor().getBounds();
        progressShell.setLocation((screenSize.width - progressShell.getBounds().width) / 2, (screenSize.height - progressShell.getBounds().height) / 2);

        progressShell.open();

        progressLabel.setText("Counting workspace files...");
        final int totalFiles = countWorkspaceFiles(oldDir);
        progressBar.setMinimum(0);
        progressBar.setMaximum(totalFiles);

        final DBRProgressMonitor monitor = new BaseProgressMonitor() {
            int filesProcessed = 0;
            @Override
            public void subTask(final String name) {
                display.syncExec(new Runnable() {
                    @Override
                    public void run() {
                        progressLabel.setText(name);
                    }
                });
            }
            @Override
            public void worked(final int work) {
                display.syncExec(new Runnable() {
                    @Override
                    public void run() {
                        filesProcessed += work;
                        progressBar.setSelection(filesProcessed);
                    }
                });
            }
        };
        new Thread() {
            @Override
            public void run() {
                try {
                    copyWorkspaceFiles(monitor, DIR_TYPE.WORKSPACE, oldDir, newDir);
                } finally {
                    display.syncExec(new Runnable() {
                        @Override
                        public void run() {
                            progressShell.dispose();
                        }
                    });
                }
            }
        }.start();

        while (!progressShell.isDisposed ()) {
            if (!display.readAndDispatch ()) display.sleep ();
        }

        application.showMessageBox("Import completed", "Configuration was imported to '" + newDir.getAbsolutePath() + "'", SWT.ICON_INFORMATION | SWT.OK);
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
                count++;
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

        int skippedFiles = 0;
        for (File file : files) {

            DIR_TYPE dirType = DIR_TYPE.NORMAL;
            switch (file.getName()) {
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

            monitor.subTask(file.getName());
            if (file.isDirectory()) {
                if (parentDirType == DIR_TYPE.METADATA && !file.getName().equals(".plugins")) {
                    // Skip all dirs but plugins
                    skippedFiles += countWorkspaceFiles(file);
                    continue;
                }
                if (parentDirType == DIR_TYPE.PLUGINS) {
                    if (!ArrayUtils.contains(COPY_PLUGINS, file.getName())) {
                        skippedFiles += countWorkspaceFiles(file);
                        continue;
                    }
                }
                if (parentDirType == DIR_TYPE.CORE) {
                    // Copy drivers in the separate location
                    switch (file.getName()) {
                        case "drivers":
                        case "maven":
                        case "remote":
                            File targetDir = new File(driversFolder, file.getName());
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
                File newDir = new File(toDir, file.getName());
                if (newDir.exists() || newDir.mkdir()) {
                    copyWorkspaceFiles(monitor, dirType, file, newDir);
                } else {
                    System.err.println("Can't create folder " + newDir.getAbsolutePath());
                }
            } else {
                if (parentDirType == DIR_TYPE.METADATA && file.getName().startsWith(".")) {
                    // Skip ALL hidden files in .metadata
                    skippedFiles++;
                    continue;
                }
                File newFile = new File(toDir, file.getName());
                if (file.getName().equals("org.jkiss.dbeaver.core.prefs")) {
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
                } else if (parentDirType == DIR_TYPE.CORE && file.getName().equals("drivers.xml")) {
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
            monitor.worked(1 + skippedFiles);
        }
    }

    private void copyFileContents(File file, File newFile) {
        // Just copy the file
        if (newFile.exists() && newFile.length() == file.length()) {
            // It is already there
        } else {
            try (FileInputStream is = new FileInputStream(file)) {
                try (FileOutputStream os = new FileOutputStream(newFile)) {
                    IOUtils.fastCopy(is, os);
                }
            } catch (IOException e) {
                // Error
                e.printStackTrace(System.err);
            }
        }
    }

    int showMessageBox(String title, String message, int style) {
        // Can't lock specified path
        MessageBox messageBox = new MessageBox(progressShell, style);
        messageBox.setText(title);
        messageBox.setMessage(message);
        return messageBox.open();
    }

}
