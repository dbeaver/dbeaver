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

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetModel;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorListenerDefault;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;


public class ConsoleViewSwitchHandler extends AbstractHandler implements IElementUpdater {
    
    private static final String CONSOLE_LOG_ENABLED_PROPERTY = "org.jkiss.dbeaver.ui.editors.sql.show.consoleView.isEnabled";
    
    @Nullable
    private static Pair<SQLConsoleLogViewer, CTabItem> findExistingLogViewer(@NotNull SQLEditor editor) {
        for (CTabItem tabItem: editor.getResultTabsContainer().getItems()) {
            if (tabItem.getData() instanceof SQLConsoleLogViewer) {
                return new Pair<SQLConsoleLogViewer, CTabItem>((SQLConsoleLogViewer) tabItem.getData(), tabItem);
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
        item.setText("Console view"); //TODO: localization
        item.setToolTipText(""); //TODO: localization
        item.setImage(DBeaverIcons.getImageDescriptor(UIIcon.SQL_CONSOLE).createImage());
        item.setData(viewer);
        item.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                if (tabsContainer.getItemCount() == 0) {
                    if (!editor.hasMaximizedControl()) {
                        // Hide results
                        editor.toggleResultPanel(false, true);
                    }
                }
            }
        });
        
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
    
    private static void subscribeEditorData(@NotNull SQLEditor editor) {
        editor.addListener(new SQLEditorListenerDefault() {
            @Override
            public void onDataReceived(DBPPreferenceStore contextPrefStore, ResultSetModel resultSet, String name) {
                if (isLogViewerEnabledForEditor(editor)) {
                    if (CommonUtils.isNotEmpty(name)) {
                        SQLConsoleLogViewer viewer = obtainLogViewer(editor).getFirst();
                        viewer.printGrid(contextPrefStore, resultSet, name);
                    }
                }
            }
        });
    }
    
    @Override
    public Object execute(@NotNull ExecutionEvent event) {
        SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
        if (editor == null) {
            return null;
        }

        Command command = event.getCommand();
        if (isLogViewerEnabledForEditor(editor)) {
            Pair<SQLConsoleLogViewer, CTabItem> viewerAndTab = findExistingLogViewer(editor);
            if (viewerAndTab != null) {
                viewerAndTab.getSecond().dispose();
                viewerAndTab.getFirst().dispose();
            }
            setLogViewerEnabledForEditor(editor, false);
        } else {
            if (!isLogViewerEnabledSetForEditor(editor)) {
                subscribeEditorData(editor);
            }
            setLogViewerEnabledForEditor(editor, true);
            if (editor.hasMaximizedControl()) {
                editor.toggleResultPanel(true, false);
            }
            editor.getResultTabsContainer().setSelection(obtainLogViewer(editor).getSecond());
        }

        ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
        commandService.refreshElements(command.getId(), null);
        return null;
    }
    
    @Override
    public void updateElement(@NotNull UIElement element, @NotNull Map parameters) {
        IWorkbenchPage activePage = UIUtils.getActiveWorkbenchWindow().getActivePage();
        if (activePage != null) {
            IEditorPart activeEditor = activePage.getActiveEditor();
            if (activeEditor instanceof SQLEditor) {
                element.setChecked(isLogViewerEnabledForEditor((SQLEditor) activeEditor));
            }
        }
    }
    
    private static boolean isLogViewerEnabledSetForEditor(@NotNull SQLEditor editor) {
        return editor.getPartProperty(CONSOLE_LOG_ENABLED_PROPERTY) != null;
    }
    
    private static boolean isLogViewerEnabledForEditor(@NotNull SQLEditor editor) {
        String value = editor.getPartProperty(CONSOLE_LOG_ENABLED_PROPERTY);
        return value != null && value.length() > 0;
    }
    
    private static void setLogViewerEnabledForEditor(@NotNull SQLEditor editor, boolean enabled) {
        if (enabled) {
            editor.setPartProperty(CONSOLE_LOG_ENABLED_PROPERTY, CONSOLE_LOG_ENABLED_PROPERTY);
        } else {
            editor.setPartProperty(CONSOLE_LOG_ENABLED_PROPERTY, "");
        }
    }
}

