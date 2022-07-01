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
package org.jkiss.dbeaver.data.console;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetModel;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorListenerDefault;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;


public class ConsoleViewSwitchHandler extends AbstractHandler {
    
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.data.console";

    private static final String CONSOLE_LOG_ENABLED_PROPERTY = "org.jkiss.dbeaver.ui.editors.sql.show.consoleView.isEnabled";
    private static final String CONSOLE_LOG_ENABLED_VALUE_TRUE = "true";
    private static final String CONSOLE_LOG_ENABLED_VALUE_FALSE = "false";
    private static final String CONSOLE_LOG_ENABLED_VALUE_DEFAULT = "default";
    
    private static final QualifiedName FILE_CONSOLE_LOG_ENABLED_PROP_NAME = new QualifiedName(BUNDLE_NAME, CONSOLE_LOG_ENABLED_PROPERTY);

    @Nullable
    private static Pair<SQLConsoleLogViewer, CTabItem> findExistingLogViewer(@NotNull SQLEditor editor) {
        for (CTabItem tabItem: editor.getResultTabsContainer().getItems()) {
            if (tabItem.getData() instanceof SQLConsoleLogViewer) {
                return new Pair<>((SQLConsoleLogViewer) tabItem.getData(), tabItem);
            }
        }
        return null;
    }

    @NotNull
    private static Pair<SQLConsoleLogViewer, CTabItem> createLogViewer(@NotNull SQLEditor editor) {
        CTabFolder tabsContainer = editor.getResultTabsContainer();
        SQLConsoleLogViewer viewer = new SQLConsoleLogViewer(editor.getSite(), tabsContainer, SWT.NONE);
        CTabItem item = new CTabItem(tabsContainer, SWT.CLOSE);
        item.setControl(viewer.getControl());
        item.setText(ConsoleMessages.console_view_item_text);
        item.setToolTipText("");
        item.setImage(DBeaverIcons.getImageDescriptor(UIIcon.SQL_CONSOLE).createImage());
        item.setData(viewer);
        item.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                if (tabsContainer.getItemCount() == 0 && !editor.hasMaximizedControl()) {
                    // Hide results
                    editor.toggleResultPanel(false, true);
                }
            }
        });
        editor.getResultTabsContainer().setSelection(item);
        UIUtils.disposeControlOnItemDispose(item);
        return new Pair<>(viewer, item);
    }
    
    @NotNull
    private static Pair<SQLConsoleLogViewer, CTabItem> obtainLogViewer(@NotNull SQLEditor editor) {
        Pair<SQLConsoleLogViewer, CTabItem> viewerAndTab = findExistingLogViewer(editor);
        if (viewerAndTab == null) {
            return createLogViewer(editor);
        } else {
            return viewerAndTab;
        }
    }
    
    static void subscribeEditorData(@NotNull SQLEditor editor) {
        editor.addListener(new SQLEditorListenerDefault() {
            @Override
            public void onDataReceived(DBPPreferenceStore contextPrefStore, ResultSetModel resultSet, String name) {
                if (isLogViewerEnabledForEditor(editor) && CommonUtils.isNotEmpty(name)) {
                    SQLConsoleLogViewer viewer = obtainLogViewer(editor).getFirst();
                    viewer.printGrid(contextPrefStore, resultSet, name);
                }
            }
        });
    }
    
    @Override
    public Object execute(@NotNull ExecutionEvent event) {
        SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
        if (editor != null) {
            toggleConsoleViewForEditor(editor);
        }
        return null;
    }

    public static void toggleConsoleViewForEditor(@NotNull SQLEditor editor) {
        boolean wasEnabled = isLogViewerEnabledForEditor(editor);
        if (wasEnabled) {
            Pair<SQLConsoleLogViewer, CTabItem> viewerAndTab = findExistingLogViewer(editor);
            if (viewerAndTab != null) {
                viewerAndTab.getSecond().dispose();
                viewerAndTab.getFirst().dispose();
            }
            setLogViewerEnabledForEditor(editor, false);
        } else {
            setLogViewerEnabledForEditor(editor, true);
            if (editor.hasMaximizedControl()) {
                editor.toggleResultPanel(true, false);
            }
            obtainLogViewer(editor);
        }        
        editor.getActivePreferenceStore().firePropertyChangeEvent(SQLConsoleViewPreferenceConstants.SHOW_CONSOLE_VIEW_BY_DEFAULT, wasEnabled, !wasEnabled);
    }

    static boolean isLogViewerEnabledSetForEditor(@NotNull SQLEditor editor) {
        return editor.getPartProperty(CONSOLE_LOG_ENABLED_PROPERTY) != null;
    }
    
    public static boolean isLogViewerEnabledForEditor(@NotNull SQLEditor editor) {
        String value = editor.getPartProperty(CONSOLE_LOG_ENABLED_PROPERTY);
        if (value == null) {
            value = CONSOLE_LOG_ENABLED_VALUE_DEFAULT;
        }
        switch (value) {
            case CONSOLE_LOG_ENABLED_VALUE_TRUE: return true;
            case CONSOLE_LOG_ENABLED_VALUE_FALSE: return false;
            case CONSOLE_LOG_ENABLED_VALUE_DEFAULT: // fall through
            default:
                {
                    IFile activeFile = EditorUtils.getFileFromInput(editor.getEditorInput());
                    if (activeFile != null) {
                        try {
                            String fileValue = activeFile.getPersistentProperty(FILE_CONSOLE_LOG_ENABLED_PROP_NAME);
                            if (fileValue != null) {
                                return fileValue.equals(CONSOLE_LOG_ENABLED_VALUE_TRUE);
                            }
                        } catch (CoreException e) {
                            e.printStackTrace();
                        }
                    }
                    return editor.getActivePreferenceStore().getBoolean(SQLConsoleViewPreferenceConstants.SHOW_CONSOLE_VIEW_BY_DEFAULT);
                }
        }
    }
    
    private static void setLogViewerEnabledForEditor(@NotNull SQLEditor editor, @Nullable Boolean enabled) {
        if (enabled == null) {
            editor.setPartProperty(CONSOLE_LOG_ENABLED_PROPERTY, CONSOLE_LOG_ENABLED_VALUE_DEFAULT);
        } else {
            String value = enabled ? CONSOLE_LOG_ENABLED_VALUE_TRUE : CONSOLE_LOG_ENABLED_VALUE_FALSE;
            editor.setPartProperty(CONSOLE_LOG_ENABLED_PROPERTY, value);
            
            IFile activeFile = EditorUtils.getFileFromInput(editor.getEditorInput());
            if (activeFile != null) {
                try {
                    activeFile.setPersistentProperty(FILE_CONSOLE_LOG_ENABLED_PROP_NAME, value);
                } catch (CoreException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    

    public static void watchForEditor(@NotNull SQLEditor editor) {
        if (!ConsoleViewSwitchHandler.isLogViewerEnabledSetForEditor(editor)) {
            ConsoleViewSwitchHandler.subscribeEditorData(editor);
        }
    }
}

