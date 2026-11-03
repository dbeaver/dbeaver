/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.acl;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.access.DBAAccessChangeCommand;
import org.jkiss.dbeaver.model.access.DBAAccessState;
import org.jkiss.dbeaver.model.access.DBAAccessSubject;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAdapter;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorUtils;
import org.jkiss.dbeaver.ui.editors.acl.internal.AccessMessages;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class PrincipalAccessEditor extends AbstractDatabaseObjectEditor<DBAAccessSubject> {
    private record StateKey(@NotNull DBAAccessSubject subject) {
    }

    private static class EditorState {
        private List<DBAAccessState.Scope> scopes = List.of();
        private String selectedScopeId;
        private final List<PrincipalAccessEditor> editors = new ArrayList<>();
        private final Map<String, DBAAccessState> scopesState = new LinkedHashMap<>();
    }

    private ObjectEditorPageControl page;
    private Composite content;
    private Combo scopes;
    private PrincipalAccessControl access;
    private EditorState state;
    private StateKey stateKey;
    private DBAAccessState current;
    private boolean loading;
    private int generation;
    private DBECommandAdapter commandListener;

    protected abstract boolean isRolesEditor();

    @Override
    public void createPartControl(@NotNull Composite parent) {
        page = new ObjectEditorPageControl(parent, SWT.NONE, this) {
            @Override
            public void fillCustomActions(@NotNull IContributionManager manager) {
                super.fillCustomActions(manager);
                DatabaseEditorUtils.contributeStandardEditorActions(getSite(), manager);
                access.contributeActions(manager);
            }
        };
        content = UIUtils.createComposite(page, 1);
        content.setLayoutData(new GridData(GridData.FILL_BOTH));
        scopes = UIUtils.createLabelCombo(content, AccessMessages.access_scope, SWT.DROP_DOWN | SWT.READ_ONLY);
        scopes.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> loadSelectedScope()));
        access = new PrincipalAccessControl(content, isRolesEditor(), this::apply);
        access.setLayoutData(new GridData(GridData.FILL_BOTH));
        page.createOrSubstituteProgressPanel(getSite());
        stateKey = new StateKey(getDatabaseObject());
        state = (EditorState) getEditorInput().getCommandContext().getUserParams().computeIfAbsent(stateKey, key -> new EditorState());
        state.editors.add(this);
        commandListener = new DBECommandAdapter() {
            @Override
            public void onSave() {
                UIUtils.asyncExec(() -> {
                    if (!page.isDisposed() && !getEditorInput().getCommandContext().isDirty()) {
                        reload();
                    }
                });
            }

            @Override
            public void onReset() {
                UIUtils.asyncExec(() -> {
                    if (!page.isDisposed()) {
                        reload();
                    }
                });
            }
        };
        getEditorInput().getCommandContext().addCommandListener(commandListener);
        showState();
    }

    @Override
    public void activatePart() {
        if (page == null) {
            return;
        }
        page.activate(true);
        if (!loading) {
            if (scopes.getItemCount() == 0) {
                loadScopes();
            } else {
                fillScopes();
            }
        }
    }

    @Override
    public void deactivatePart() {
        if (page != null) {
            page.activate(false);
        }
    }

    private void loadScopes() {
        if (!state.scopes.isEmpty()) {
            fillScopes();
            return;
        }
        load(monitor -> getDatabaseObject().getAccessManager().getScopes(monitor), result -> {
            state.scopes = result;
            fillScopes();
        });
    }

    private void fillScopes() {
        String selected = state.selectedScopeId;
        scopes.removeAll();
        for (DBAAccessState.Scope scope : state.scopes) {
            scopes.add(scope.label());
        }
        int index = 0;
        for (int scopeIndex = 0; scopeIndex < state.scopes.size(); scopeIndex++) {
            if (state.scopes.get(scopeIndex).id().equals(selected)) {
                index = scopeIndex;
            }
        }
        if (!state.scopes.isEmpty()) {
            scopes.select(index);
            loadSelectedScope();
        }
    }

    private void loadSelectedScope() {
        int index = scopes.getSelectionIndex();
        if (index < 0) {
            return;
        }
        DBAAccessState.Scope scope = state.scopes.get(index);
        state.selectedScopeId = scope.id();
        DBAAccessState cached = state.scopesState.get(scope.id());
        current = null;
        showState();
        if (cached != null) {
            current = cached;
            showState();
        } else {
            load(monitor -> getDatabaseObject().getAccessManager().load(monitor, scope), result -> {
                state.scopesState.putIfAbsent(scope.id(), result);
                current = state.scopesState.get(scope.id());
                showState();
            });
        }
    }

    private interface Loader<RESULT> {
        @NotNull
        RESULT load(@NotNull DBRProgressMonitor monitor) throws DBException;
    }

    private <RESULT> void load(@NotNull Loader<RESULT> loader, @NotNull Consumer<RESULT> consumer) {
        int version = ++generation;
        loading = true;
        content.setEnabled(false);
        page.runService(LoadingJob.createService(new DatabaseLoadService<RESULT>(AccessMessages.access_load, getExecutionContext()) {
            @NotNull
            @Override
            public RESULT evaluate(@NotNull DBRProgressMonitor monitor) throws InvocationTargetException {
                try {
                    return loader.load(monitor);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            }
        }, page.new ProgressVisualizer<RESULT>() {
            @Override
            public void completeLoading(@Nullable RESULT result) {
                super.completeLoading(result);
                if (!page.isDisposed() && generation == version) {
                    loading = false;
                    content.setEnabled(true);
                    if (result != null) {
                        consumer.accept(result);
                    }
                }
            }
        }));
    }

    private void apply(@NotNull DBAAccessState changed) {
        if (changed.equals(current)) {
            return;
        }
        DBAAccessChangeCommand command = new DBAAccessChangeCommand(getDatabaseObject(), AccessMessages.access_change, current, changed);
        setCurrent(changed);
        addChangeCommand(command, new DBECommandReflector<DBAAccessSubject, DBAAccessChangeCommand>() {
            @Override
            public void redoCommand(@NotNull DBAAccessChangeCommand command) {
                UIUtils.asyncExec(() -> setCurrent(command.getAfter()));
            }

            @Override
            public void undoCommand(@NotNull DBAAccessChangeCommand command) {
                UIUtils.asyncExec(() -> setCurrent(command.getBefore()));
            }
        });
        firePropertyChange(PROP_DIRTY);
    }

    private void setCurrent(@NotNull DBAAccessState changed) {
        state.scopesState.put(changed.scope().id(), changed);
        for (PrincipalAccessEditor editor : state.editors) {
            if (!editor.page.isDisposed() && editor.scopes.getSelectionIndex() >= 0
                && state.scopes.get(editor.scopes.getSelectionIndex()).id().equals(changed.scope().id())) {
                editor.current = changed;
                editor.showState();
            }
        }
    }

    private void showState() {
        if (access != null && !access.isDisposed()) {
            access.setEditable(getExecutionContext() != null && !DBUtils.isReadOnly(getDatabaseObject()));
            access.setState(current);
        }
    }

    private void reload() {
        getEditorInput().getCommandContext().getUserParams().put(stateKey, state);
        state.scopesState.clear();
        state.scopes = List.of();
        current = null;
        scopes.removeAll();
        showState();
        loading = false;
        loadScopes();
    }

    @NotNull
    @Override
    public RefreshResult refreshPart(@Nullable Object source, boolean force) {
        if (getEditorInput().getCommandContext().isDirty()) {
            return RefreshResult.IGNORED;
        }
        reload();
        return RefreshResult.REFRESHED;
    }

    @Override
    public void setFocus() {
        if (access != null) {
            access.setFocus();
        }
    }

    @Override
    public void dispose() {
        if (commandListener != null) {
            getEditorInput().getCommandContext().removeCommandListener(commandListener);
        }
        if (state != null) {
            state.editors.remove(this);
        }
        generation++;
        super.dispose();
    }
}
