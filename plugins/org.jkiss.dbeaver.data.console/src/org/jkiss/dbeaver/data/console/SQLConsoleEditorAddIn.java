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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
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
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorListener;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorListenerDefault;
import org.jkiss.dbeaver.ui.editors.sql.addins.SQLEditorAddIn;
import org.jkiss.utils.CommonUtils;

import java.io.PrintWriter;


public class SQLConsoleEditorAddIn implements SQLEditorAddIn {
    private static final Log log = Log.getLog(ConsoleViewSwitchHandler.class);

    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.data.console";

    private static final String CONSOLE_VIEW_ENABLED_PROPERTY = "org.jkiss.dbeaver.ui.editors.sql.show.consoleView.isEnabled";
    private static final String CONSOLE_VIEW_ENABLED_VALUE_TRUE = "true";
    private static final String CONSOLE_VIEW_ENABLED_VALUE_FALSE = "false";
    private static final String CONSOLE_VIEW_ENABLED_VALUE_DEFAULT = "default";
    
    private static final QualifiedName FILE_CONSOLE_VIEW_ENABLED_PROP_NAME = new QualifiedName(BUNDLE_NAME, CONSOLE_VIEW_ENABLED_PROPERTY);

    private SQLEditor editor;
    private ConsoleViewContext viewContext;
    
    private final SQLEditorListener editorListener = new SQLEditorListenerDefault() {
        @Override
        public void onDataReceived(@NotNull DBPPreferenceStore contextPrefStore, @NotNull ResultSetModel resultSet, String name) {
            if (isConsoleViewEnabled() && CommonUtils.isNotEmpty(name)) {
                obtainViewContext().view.printQueryData(contextPrefStore, resultSet, name);
            }
        }

        @Override
        public void onQueryResult(@NotNull DBPPreferenceStore contextPrefStore, @NotNull SQLQueryResult result) {
            if (isConsoleViewEnabled()) {
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
        if (editor.getActivePreferenceStore().getBoolean(SQLConsoleViewPreferenceConstants.SHOW_SERVER_OUTPUT) && isConsoleViewEnabled()) {
            return UIUtils.syncExec(new RunnableWithResult<>() {
                public PrintWriter runWithResult() {
                    return obtainViewContext().view.getOutputWriter();
                }
            });
        } else {
            return null;
        }
    }
        
    private class ConsoleViewContext {
        public final SQLConsoleView view;
        public final CTabItem tabItem;
        
        public ConsoleViewContext() {
            CTabFolder tabsContainer = editor.getResultTabsContainer();
            view = new SQLConsoleView(editor.getSite(), tabsContainer, SWT.NONE);
            tabItem = new CTabItem(tabsContainer, SWT.CLOSE);
            tabItem.setControl(view.getControl());
            tabItem.setText(ConsoleMessages.console_view_item_text);
            tabItem.setToolTipText("");
            tabItem.setImage(DBeaverIcons.getImageDescriptor(UIIcon.SQL_CONSOLE).createImage());
            tabItem.setData(view);
            tabsContainer.addCTabFolder2Listener(new CTabFolder2Adapter() {
                @Override
                public void close(CTabFolderEvent event) {
                    Widget item = event.item;
                    if (item instanceof CTabItem) {
                        CTabItem cTab = (CTabItem) item;
                        if (cTab.getData() instanceof SQLConsoleView) {
                            setConcoleViewEnabled(false);
                        }
                    }
                }
            });
            tabItem.addDisposeListener(e -> {
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
    private ConsoleViewContext obtainViewContext() {
        if (viewContext == null) {
            viewContext = new ConsoleViewContext();
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
    public void toggleConsoleView() {
        boolean wasEnabled = isConsoleViewEnabled();
        if (wasEnabled) {
            if (viewContext != null) {
                viewContext.dispose();
            }
            setConcoleViewEnabled(false);
        } else {
            setConcoleViewEnabled(true);
            if (editor.hasMaximizedControl()) {
                editor.toggleResultPanel(true, false);
            }
            obtainViewContext();
        }        
        editor.getActivePreferenceStore().firePropertyChangeEvent(
            SQLConsoleViewPreferenceConstants.SHOW_CONSOLE_VIEW_BY_DEFAULT,
            wasEnabled, !wasEnabled
        );
    }

    /**
     * Checks whether the console view is enabled for the current editor.
     * SQL Terminal state is stored in the editor properties in the runtime
     * and saved in the script file to keep the state between launches.
     */
    public boolean isConsoleViewEnabled() {
        String value = editor.getPartProperty(CONSOLE_VIEW_ENABLED_PROPERTY);
        if (value == null) {
            value = CONSOLE_VIEW_ENABLED_VALUE_DEFAULT;
        }
        switch (value) {
            case CONSOLE_VIEW_ENABLED_VALUE_TRUE: return true;
            case CONSOLE_VIEW_ENABLED_VALUE_FALSE: return false;
            case CONSOLE_VIEW_ENABLED_VALUE_DEFAULT: // fall through
            default: {
                boolean enabled = getInitialConsoleViewEnabled();
                editor.setResultSetAutoFocusEnabled(!enabled);
                return enabled;
            }
        }
    }
    
    private boolean getInitialConsoleViewEnabled() {
        IFile activeFile = EditorUtils.getFileFromInput(editor.getEditorInput());
        if (activeFile != null) {
            try {
                String fileValue = activeFile.getPersistentProperty(FILE_CONSOLE_VIEW_ENABLED_PROP_NAME);
                if (fileValue != null) {
                    return fileValue.equals(CONSOLE_VIEW_ENABLED_VALUE_TRUE);
                }
            } catch (CoreException e) {
                log.debug(e.getMessage(), e);
            }
        }
        return editor.getActivePreferenceStore().getBoolean(SQLConsoleViewPreferenceConstants.SHOW_CONSOLE_VIEW_BY_DEFAULT);
    }
    
    private void setConcoleViewEnabled(@Nullable Boolean enabled) {
        if (enabled == null) {
            editor.setPartProperty(CONSOLE_VIEW_ENABLED_PROPERTY, CONSOLE_VIEW_ENABLED_VALUE_DEFAULT);
        } else {
            String value = enabled ? CONSOLE_VIEW_ENABLED_VALUE_TRUE : CONSOLE_VIEW_ENABLED_VALUE_FALSE;
            editor.setPartProperty(CONSOLE_VIEW_ENABLED_PROPERTY, value);
            editor.setResultSetAutoFocusEnabled(!enabled);
            IFile activeFile = EditorUtils.getFileFromInput(editor.getEditorInput());
            if (activeFile != null) {
                try {
                    activeFile.setPersistentProperty(FILE_CONSOLE_VIEW_ENABLED_PROP_NAME, value);
                } catch (CoreException e) {
                    log.debug(e.getMessage(), e);
                }
            }
        }
    }
}
