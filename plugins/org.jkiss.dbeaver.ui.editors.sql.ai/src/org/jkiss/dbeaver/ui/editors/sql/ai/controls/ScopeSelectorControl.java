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
package org.jkiss.dbeaver.ui.editors.sql.ai.controls;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionScope;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionSettings;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSStructContainer;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.*;

public class ScopeSelectorControl extends Composite {
    private static final Log log = Log.getLog(ScopeSelectorControl.class);

    private DBSLogicalDataSource dataSource;
    private DBCExecutionContext executionContext;

    private final Combo scopeCombo;
    private final Text scopeText;
    private final ToolItem scopeConfigItem;
    private final ToolBar toolBar;

    private final Set<String> checkedObjectIds;
    private DAICompletionScope currentScope;

    public ScopeSelectorControl(
        @NotNull Composite parent,
        @NotNull DBSLogicalDataSource dataSource,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DAICompletionSettings settings
    ) {
        super(parent, SWT.NONE);

        setLayout(GridLayoutFactory.fillDefaults().margins(0, 0).numColumns(5).create());

        this.dataSource = dataSource;
        this.executionContext = executionContext;
        this.currentScope = settings.getScope();
        this.checkedObjectIds = new HashSet<>();

        if (!ArrayUtils.isEmpty(settings.getCustomObjectIds())) {
            checkedObjectIds.addAll(Arrays.asList(settings.getCustomObjectIds()));
        }

        scopeCombo = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (DAICompletionScope scope : DAICompletionScope.values()) {
            scopeCombo.add(scope.getTitle());
            if (currentScope == scope) {
                scopeCombo.select(scopeCombo.getItemCount() - 1);
            }
        }
        scopeCombo.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                changeScope(CommonUtils.fromOrdinal(DAICompletionScope.class, scopeCombo.getSelectionIndex()));
            }
        });

        scopeText = new Text(this, SWT.READ_ONLY | SWT.BORDER);
        scopeText.setEditable(false);
        scopeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        toolBar = new ToolBar(this, SWT.FLAT);
        toolBar.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        scopeConfigItem = UIUtils.createToolItem(
            toolBar,
            "Customize",
            UIIcon.RS_DETAILS,
            SelectionListener.widgetSelectedAdapter(e -> changeScope(DAICompletionScope.CUSTOM))
        );

        showScopeSettings(currentScope);
    }

    public void setInput(@NotNull DBSLogicalDataSource dataSource, @NotNull DBCExecutionContext executionContext) {
        this.dataSource = dataSource;
        this.executionContext = executionContext;
        showScopeSettings(currentScope);
    }

    @NotNull
    public ToolBar getToolBar() {
        return toolBar;
    }

    @NotNull
    public Combo getScopeCombo() {
        return scopeCombo;
    }

    @NotNull
    public Text getScopeText() {
        return scopeText;
    }

    @NotNull
    public Set<String> getCheckedObjectIds() {
        return checkedObjectIds;
    }

    @NotNull
    public DAICompletionScope getScope() {
        return currentScope;
    }

    @NotNull
    public List<DBSEntity> getCustomEntities(@NotNull DBRProgressMonitor monitor) {
        return loadCustomEntities(monitor, executionContext.getDataSource(), checkedObjectIds);
    }

    @NotNull
    public DBSLogicalDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    public DBCExecutionContext getExecutionContext() {
        return executionContext;
    }

    private void showScopeSettings(@NotNull DAICompletionScope scope) {
        final String text = switch (scope) {
            case CURRENT_SCHEMA -> {
                if (CommonUtils.isNotEmpty(dataSource.getCurrentSchema())) {
                    yield dataSource.getCurrentSchema();
                } else if (CommonUtils.isNotEmpty(dataSource.getCurrentCatalog())) {
                    yield dataSource.getCurrentCatalog();
                } else {
                    yield dataSource.getDataSourceContainer().getName();
                }
            }
            case CURRENT_DATABASE -> {
                if (CommonUtils.isNotEmpty(dataSource.getCurrentCatalog())) {
                    yield dataSource.getCurrentCatalog();
                } else {
                    yield dataSource.getDataSourceContainer().getName();
                }
            }
            case CURRENT_DATASOURCE -> dataSource.getDataSourceContainer().getName();
            default -> checkedObjectIds.size() + " object(s)";
        };

        scopeConfigItem.setEnabled(scope == DAICompletionScope.CUSTOM);
        scopeText.setText(CommonUtils.toString(text, "N/A"));

        requestLayout();
        layout(true, true);
    }

    @NotNull
    public static List<DBSEntity> loadCustomEntities(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull Set<String> ids
    ) {
        List<DBSEntity> entities = new ArrayList<>();
        try {
            if (dataSource instanceof DBSObjectContainer) {
                monitor.beginTask("Load custom entities", 1);
                try {
                    loadCheckedEntitiesById(monitor, (DBSObjectContainer) dataSource, ids, entities);
                } finally {
                    monitor.done();
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
        return entities;
    }


    private static void loadCheckedEntitiesById(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObjectContainer container,
        @NotNull Set<String> ids,
        @NotNull List<DBSEntity> output
    ) throws DBException {
        Collection<? extends DBSObject> children = container.getChildren(monitor);

        if (children != null) {
            for (DBSObject child : children) {
                if (child instanceof DBSEntity) {
                    if (ids.contains(DBUtils.getObjectFullId(child))) {
                        output.add((DBSEntity) child);
                    }
                } else if (child instanceof DBSStructContainer) {
                    loadCheckedEntitiesById(monitor, (DBSObjectContainer) child, ids, output);
                }
            }
        }
    }

    public void changeScope(@NotNull DAICompletionScope scope) {
        if (scope == DAICompletionScope.CUSTOM) {
            final ScopeConfigDialog dialog = new ScopeConfigDialog(getShell(), checkedObjectIds, executionContext.getDataSource());
            if (dialog.open() != IDialogConstants.OK_ID) {
                return;
            }

            checkedObjectIds.clear();
            checkedObjectIds.addAll(dialog.getCheckedObjectIds());
        }

        currentScope = scope;
        showScopeSettings(scope);
    }

}
