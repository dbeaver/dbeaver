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
package org.jkiss.dbeaver.ui.editors.sql.terminal;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Widget;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.RunnableWithResult;
import org.jkiss.dbeaver.model.sql.SQLQueryResult;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetModel;
import org.jkiss.dbeaver.ui.editors.EditorPartContextualProperty;
import org.jkiss.dbeaver.ui.editors.EditorPartContextualProperty.PartCustomPropertyValueInfo;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorListener;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorListenerDefault;
import org.jkiss.dbeaver.ui.editors.sql.addins.SQLEditorAddIn;
import org.jkiss.dbeaver.ui.editors.sql.terminal.internal.SQLTerminalMessages;
import org.jkiss.utils.CommonUtils;

import java.io.PrintWriter;
import java.util.Map;


public class SQLTerminalEditorAddIn implements SQLEditorAddIn {
    private static final Log log = Log.getLog(SQLTerminalViewSwitchHandler.class);

    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.editors.sql.terminal";

    private static final String TERMINAL_VIEW_ENABLED_PROPERTY = "org.jkiss.dbeaver.ui.editors.sql.show.consoleView.isEnabled";
    
    private static final QualifiedName FILE_TERMINAL_VIEW_ENABLED_PROP_NAME = new QualifiedName(
        BUNDLE_NAME, TERMINAL_VIEW_ENABLED_PROPERTY
    );
    
    private static final EditorPartContextualProperty terminalViewEnabledPartProperty = EditorPartContextualProperty.setup(
        TERMINAL_VIEW_ENABLED_PROPERTY, FILE_TERMINAL_VIEW_ENABLED_PROP_NAME, 
        SQLTerminalPreferencesConstants.SHOW_TERMINAL_VIEW_BY_DEFAULT, CommonUtils.toString(false));

    private SQLEditor editor;
    private TerminalViewContext viewContext;
    
    private final SQLEditorListener editorListener = new SQLEditorListenerDefault() {
        @Override
        public void onDataReceived(@NotNull DBPPreferenceStore contextPrefStore, @NotNull ResultSetModel resultSet, String name) {
            if (isTerminalViewEnabled() && CommonUtils.isNotEmpty(name)) {
                obtainViewContext().view.printQueryData(contextPrefStore, resultSet, name);
            }
        }

        @Override
        public void onQueryResult(@NotNull DBPPreferenceStore contextPrefStore, @NotNull SQLQueryResult result) {
            if (isTerminalViewEnabled()) {
                obtainViewContext().view.printQueryResult(contextPrefStore, result);
            }
        }
    };
    
    @Override
    public void init(@NotNull SQLEditor editor) {
        this.editor = editor;
        this.viewContext = null;
        
        this.editor.addListener(editorListener);
    }

    @Override
    public void cleanup(@NotNull SQLEditor editor) {
        this.editor.removeListener(editorListener);
    }

    @Nullable
    @Override
    public PrintWriter getServerOutputConsumer() {
        return UIUtils.syncExec(new RunnableWithResult<>() {
            public PrintWriter runWithResult() {
                if (editor.getActivePreferenceStore().getBoolean(SQLTerminalPreferencesConstants.SHOW_SERVER_OUTPUT) && isTerminalViewEnabled()) {
                    return obtainViewContext().view.getOutputWriter();
                } else {
                    return null;
                }
            }
        });
    }
        
    private class TerminalViewContext {
        public final SQLTerminalView view;
        public final CTabItem tabItem;
        
        public TerminalViewContext() {
            CTabFolder tabsContainer = editor.getResultTabsContainer();
            view = new SQLTerminalView(editor.getSite(), tabsContainer, SWT.NONE);
            tabItem = new CTabItem(tabsContainer, SWT.CLOSE);
            tabItem.setControl(view.getControl());
            tabItem.setText(SQLTerminalMessages.sql_terminal_item_text);
            tabItem.setToolTipText("");
            tabItem.setImage(DBeaverIcons.getImage(UIIcon.SQL_CONSOLE));
            tabItem.setData(view);
            tabsContainer.addCTabFolder2Listener(new CTabFolder2Adapter() {
                @Override
                public void close(CTabFolderEvent event) {
                    Widget item = event.item;
                    if (item instanceof CTabItem) {
                        CTabItem cTab = (CTabItem) item;
                        if (cTab.getData() instanceof SQLTerminalView) {
                            setConsoleViewEnabled(false);
                        }
                    }
                }
            });
            tabItem.addDisposeListener(e -> {
                Object item = e.getSource();
                if (item instanceof CTabItem) {
                    CTabItem cTab = (CTabItem) item;
                    if (cTab.getData() instanceof SQLTerminalView) {
                        setConsoleViewEnabled(false);
                    }
                }
                if (tabsContainer.getItemCount() == 0 && !editor.hasMaximizedControl()) {
                    // Hide results
                    editor.toggleResultPanel(false, true);
                }
                resetViewContext();
            });
            editor.getResultTabsContainer().setSelection(tabItem);
            UIUtils.disposeControlOnItemDispose(tabItem);
        }
        
        public void dispose() {
            tabItem.dispose();
            view.dispose();
        }
    }
    
    @NotNull
    private TerminalViewContext obtainViewContext() {
        if (viewContext == null && isTerminalViewEnabled()) {
            viewContext = new TerminalViewContext();
        } 
        return viewContext;
    }
    
    private void resetViewContext() {
        viewContext = null;
    }

    /**
     * Changes the state of SQL Terminal.
     * The state is stored in the editor properties.
     */
    public void toggleTerminalView() {
        boolean wasEnabled = isTerminalViewEnabled();
        if (wasEnabled) {
            if (viewContext != null) {
                viewContext.dispose();
            }
            setConsoleViewEnabled(false);
        } else {
            setConsoleViewEnabled(true);
            if (editor.hasMaximizedControl()) {
                editor.toggleResultPanel(true, false);
            }
            obtainViewContext();
        }        
        editor.getActivePreferenceStore().firePropertyChangeEvent(
            SQLTerminalPreferencesConstants.SHOW_TERMINAL_VIEW_BY_DEFAULT,
            wasEnabled, !wasEnabled
        );
    }

    /**
     * Checks whether the console view is enabled for the current editor.
     * SQL Terminal state is stored in the editor properties in the runtime
     * and saved in the script file to keep the state between launches.
     */
    public boolean isTerminalViewEnabled() {
        PartCustomPropertyValueInfo info = terminalViewEnabledPartProperty.getPropertyValue(editor);
        boolean enabled = CommonUtils.toBoolean(info.value);
        if (info.isInitial) {
            editor.setResultSetAutoFocusEnabled(!enabled);
        }
        return enabled;
    }
    
    private void setConsoleViewEnabled(boolean enabled) {
        if (terminalViewEnabledPartProperty.setPropertyValue(editor, CommonUtils.toString(enabled))) {
            editor.setResultSetAutoFocusEnabled(!enabled);
            SQLTerminalFeatures.SQL_TERMINAL_OPEN.use(Map.of("show", enabled));
        }
    }
}
