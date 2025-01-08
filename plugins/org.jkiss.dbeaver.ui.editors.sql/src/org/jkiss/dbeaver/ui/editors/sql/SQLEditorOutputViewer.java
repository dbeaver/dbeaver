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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.output.DBCOutputSeverity;
import org.jkiss.dbeaver.model.exec.output.DBCOutputWriter;
import org.jkiss.dbeaver.model.exec.output.DBCServerOutputReader;
import org.jkiss.dbeaver.model.exec.output.DBCServerOutputReaderExt;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.MenuCreator;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;

import java.io.PrintWriter;
import java.util.*;

/**
 * SQL editor output viewer
 */
public class SQLEditorOutputViewer extends Composite implements DBCOutputWriter {
    private static final int MAX_RECORDS = 1000;

    private final Set<DBCOutputSeverity> severities = new HashSet<>();
    private final Deque<OutputRecord> records = new ArrayDeque<>(MAX_RECORDS);

    private final Text filterText;
    private final ToolBarManager filterToolbar;
    private final SQLEditorOutputConsoleViewer viewer;

    private DBCExecutionContext executionContext;

    public SQLEditorOutputViewer(@NotNull IWorkbenchPartSite site, @NotNull Composite parent, int style) {
        super(parent, style);

        setLayoutData(new GridData(GridData.FILL_BOTH));
        setLayout(GridLayoutFactory.fillDefaults().spacing(0, 0).create());

        final Composite filterComposite = UIUtils.createPlaceholder(this, 2);
        filterComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        filterText = new Text(filterComposite, SWT.SINGLE | SWT.SEARCH | SWT.ICON_CANCEL);
        filterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        filterText.setMessage(SQLEditorMessages.sql_editor_panel_output_filter_message);
        filterText.addModifyListener(e -> filterOutput());

        filterToolbar = new ToolBarManager();
        filterToolbar.add(new ConfigureSeverityAction());
        filterToolbar.createControl(filterComposite);

        viewer = new SQLEditorOutputConsoleViewer(site, this, SWT.NONE) {
            @Override
            public void clearOutput() {
                super.clearOutput();
                records.clear();
            }
        };
        viewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

        TextEditorUtils.enableHostEditorKeyBindingsSupport(site, filterText);
        updateControls();
    }

    @Override
    public void println(@Nullable DBCOutputSeverity severity, @Nullable String message) {
        if (message == null) {
            return;
        }
        if (severity == null || severity.isForced() || severities.contains(severity)) {
            PrintWriter writer = viewer.getOutputWriter();
            if (severity != null && severity.isForced()) {
                writer.print("[" + severity.getName() + "] ");
            }
            writer.println(message);
        }
        records.offer(new OutputRecord(severity, message));
        if (records.size() > MAX_RECORDS) {
            records.pop();
        }
    }

    @Override
    public void flush() {
        viewer.getOutputWriter().flush();
    }

    @NotNull
    public SQLEditorOutputConsoleViewer getViewer() {
        return viewer;
    }

    public boolean isHasNewOutput() {
        return viewer.isHasNewOutput();
    }

    public void refreshStyles() {
        viewer.refreshStyles();
    }

    public void resetNewOutput() {
        viewer.resetNewOutput();
    }

    public void clearOutput() {
        viewer.clearOutput();
    }

    public void setExecutionContext(@Nullable DBCExecutionContext executionContext) {
        if (this.executionContext != executionContext) {
            this.executionContext = executionContext;

            updateControls();
        }
    }

    private void updateControls() {
        clearOutput();
        severities.clear();

        final DBPDataSource dataSource = executionContext != null ? executionContext.getDataSource() : null;
        final DBCServerOutputReader reader = DBUtils.getAdapter(DBCServerOutputReader.class, dataSource);

        if (reader instanceof DBCServerOutputReaderExt readerExt) {
            final DBCOutputSeverity[] supportedSeverities = readerExt.getSupportedSeverities(executionContext);

            severities.addAll(List.of(supportedSeverities));

            UIUtils.setControlVisible(filterToolbar.getControl(), supportedSeverities.length > 0);
        } else {
            UIUtils.setControlVisible(filterToolbar.getControl(), false);
        }

        filterToolbar.getControl().getParent().layout(true, true);
    }

    private void filterOutput() {
        viewer.getConsole().clearConsole();

        final PrintWriter writer = viewer.getOutputWriter();
        final String filter = filterText.getText().trim();

        for (OutputRecord record : records) {
            if (record.severity != null && !record.severity.isForced() && !severities.contains(record.severity)) {
                continue;
            }
            if (!filter.isEmpty() && !record.line.contains(filter)) {
                continue;
            }
            writer.println(record.line);
        }

        writer.flush();
    }

    private class ConfigureSeverityAction extends Action {
        public ConfigureSeverityAction() {
            super(null, AS_DROP_DOWN_MENU);

            final MenuManager filterMenu = new MenuManager();
            filterMenu.setRemoveAllWhenShown(true);
            filterMenu.addMenuListener(manager -> {
                final DBCServerOutputReader reader = DBUtils.getAdapter(DBCServerOutputReader.class, executionContext.getDataSource());
                if (!(reader instanceof DBCServerOutputReaderExt readerExt)) {
                    return;
                }
                for (DBCOutputSeverity severity : readerExt.getSupportedSeverities(executionContext)) {
                    manager.add(new ToggleSeverityAction(severity));
                }
            });

            setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.FILTER));
            setToolTipText(SQLEditorMessages.sql_editor_panel_output_filter_hint);
            setMenuCreator(new MenuCreator(control -> filterMenu));
        }
    }

    private class ToggleSeverityAction extends Action {
        private final DBCOutputSeverity severity;

        public ToggleSeverityAction(@NotNull DBCOutputSeverity severity) {
            super(severity.getName(), AS_CHECK_BOX);
            this.severity = severity;
        }

        @Override
        public boolean isChecked() {
            return severities.contains(severity);
        }

        @Override
        public void run() {
            if (severities.contains(severity)) {
                severities.remove(severity);
            } else {
                severities.add(severity);
            }

            filterOutput();
        }
    }

    private static class OutputRecord {
        private final DBCOutputSeverity severity;
        private final String line;

        public OutputRecord(@Nullable DBCOutputSeverity severity, @NotNull String line) {
            this.severity = severity;
            this.line = line;
        }
    }
}
