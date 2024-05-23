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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.DoubleClickMouseAdapter;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.StringJoiner;

public class PrefPageDriversClasspath extends AbstractPrefPage implements IWorkbenchPreferencePage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.drivers.classpath";

    private static final Log log = Log.getLog(PrefPageDriversClasspath.class);

    private List globalLibrariesList;
    private List systemClasspathList;

    @Override
    public void init(IWorkbench workbench) {
        // nothing to initialize
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        final Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        {
            final Group group = UIUtils.createControlGroup(composite, UIConnectionMessages.pref_page_drivers_classpath_global_libraries_group, 2, GridData.FILL_BOTH, 300);

            globalLibrariesList = new List(group, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
            globalLibrariesList.setLayoutData(new GridData(GridData.FILL_BOTH));
            ((GridData) globalLibrariesList.getLayoutData()).heightHint = 100;

            final ToolBar toolbar = new ToolBar(group, SWT.VERTICAL);
            toolbar.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

            UIUtils.createToolItem(toolbar, UIConnectionMessages.pref_page_drivers_button_add, UIIcon.ADD, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    final File[] files = DialogUtils.openFileList(getShell(), UIConnectionMessages.pref_page_drivers_classpath_global_libraries_choose_files, new String[]{"*.jar"});
                    if (files != null) {
                        for (File file : files) {
                            final String path = file.toString();
                            if (globalLibrariesList.indexOf(path) < 0) {
                                globalLibrariesList.add(path);
                            }
                        }
                    }
                }
            });
            final ToolItem removeButton = UIUtils.createToolItem(toolbar, UIConnectionMessages.pref_page_drivers_button_remove, UIIcon.DELETE, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    final int index = globalLibrariesList.getSelectionIndex();
                    globalLibrariesList.remove(index);
                    globalLibrariesList.select(CommonUtils.clamp(index, 0, globalLibrariesList.getItemCount() - 1));
                    globalLibrariesList.notifyListeners(SWT.Selection, new Event());
                }
            });

            globalLibrariesList.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (globalLibrariesList.getSelectionIndex() >= 0) {
                        removeButton.setEnabled(globalLibrariesList.getItemCount() >= 1);
                    } else {
                        removeButton.setEnabled(false);
                    }
                }
            });
            globalLibrariesList.addMouseListener(new DoubleClickMouseAdapter() {
                @Override
                public void onMouseDoubleClick(@NotNull MouseEvent e) {
                    if (globalLibrariesList.getSelectionIndex() >= 0) {
                        ShellUtils.showInSystemExplorer(globalLibrariesList.getSelection()[0]);
                    }
                }
            });

            UIUtils.createInfoLabel(group, UIConnectionMessages.pref_page_drivers_classpath_global_libraries_info, GridData.FILL_HORIZONTAL, 2);
        }

        {
            final Group group = UIUtils.createControlGroup(composite, UIConnectionMessages.pref_page_drivers_classpath_system_classpath_group, 1, GridData.FILL_BOTH, 300);

            systemClasspathList = new List(group, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
            systemClasspathList.setLayoutData(new GridData(GridData.FILL_BOTH));
            ((GridData) systemClasspathList.getLayoutData()).heightHint = 100;
        }

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults() {
        globalLibrariesList.removeAll();
        systemClasspathList.removeAll();

        for (String source : DriverDescriptor.getGlobalLibraries()) {
            globalLibrariesList.add(source);
        }

        for (String path : CommonUtils.splitString(System.getProperty(StandardConstants.ENV_JAVA_CLASSPATH), ';')) {
            systemClasspathList.add(path);
        }

        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        final DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        final String[] currentGlobalLibraries = DriverDescriptor.getGlobalLibraries();

        {
            final StringJoiner libraries = new StringJoiner("|");
            for (String library : globalLibrariesList.getItems()) {
                try {
                    libraries.add(URLEncoder.encode(library, GeneralUtils.UTF8_ENCODING));
                } catch (UnsupportedEncodingException e) {
                    log.error(e);
                }
            }
            store.setValue(ModelPreferences.UI_DRIVERS_GLOBAL_LIBRARIES, libraries.toString());
        }

        PrefUtils.savePreferenceStore(store);

        if (!Arrays.equals(currentGlobalLibraries, globalLibrariesList.getItems()) && DriverDescriptor.getRootClassLoader() != null) {
            final boolean restart = UIUtils.confirmAction(
                getShell(),
                NLS.bind(UIConnectionMessages.pref_page_drivers_classpath_global_libraries_restart_prompt_title, GeneralUtils.getProductName()),
                NLS.bind(UIConnectionMessages.pref_page_drivers_classpath_global_libraries_restart_prompt_body, GeneralUtils.getProductName())
            );

            if (restart) {
                restartWorkbenchOnPrefChange();
            }
        }

        return true;
    }
}
