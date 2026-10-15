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
package org.jkiss.dbeaver.ext.postgresql.ui.editors;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgrePublication;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTable;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAdapter;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.properties.ObjectAttributeDescriptor;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.List;
import java.util.Locale;

/**
 * Table selection for the ordinary, unsaved publication metadata editor.
 */
public class PostgrePublicationTablesEditor extends AbstractDatabaseObjectEditor<PostgrePublication> {
    private CheckboxTableViewer viewer;
    private Label message;
    private Text filterText;
    private AbstractJob loadJob;
    private DBECommandAdapter listener;

    @Override
    public void createPartControl(@NotNull Composite parent) {
        Composite content = UIUtils.createComposite(parent, 1);
        message = UIUtils.createControlLabel(content, PostgreMessages.dialog_create_publication_tables);
        filterText = new Text(content, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
        filterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        filterText.setMessage(PostgreMessages.dialog_create_publication_filter_tables);
        filterText.setEnabled(false);
        viewer = CheckboxTableViewer.newCheckList(content, SWT.BORDER | SWT.V_SCROLL);
        viewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
        viewer.getTable().setEnabled(false);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new LabelProvider() {
            @NotNull
            @Override
            public String getText(@NotNull Object element) {
                return ((PostgreTable) element).getFullyQualifiedName(DBPEvaluationContext.UI);
            }
        });
        viewer.addFilter(new ViewerFilter() {
            @Override
            public boolean select(@NotNull Viewer viewer, @Nullable Object parentElement, @NotNull Object element) {
                String name = ((PostgreTable) element).getFullyQualifiedName(DBPEvaluationContext.UI);
                return name.toLowerCase(Locale.ROOT).contains(filterText.getText().toLowerCase(Locale.ROOT));
            }
        });
        filterText.addModifyListener(event -> {
            viewer.refresh();
            refreshSelection();
        });
        PostgrePublication publication = getDatabaseObject();
        PropertySourceEditable source = new PropertySourceEditable(getEditorInput().getCommandContext(), publication, publication);
        // The selection is a normal undoable property change, but its collection is not rendered as a text field.
        ObjectPropertyDescriptor tables = ObjectAttributeDescriptor.extractAnnotations(source, PostgrePublication.class, null, null)
            .stream().filter(property -> PostgrePublication.PROP_ID_CREATION_TABLES.equals(property.getId())).findFirst().orElseThrow();
        viewer.addCheckStateListener(event -> {
            if (!publication.isPersisted()) {
                source.setPropertyValue(null, publication, tables,
                    PostgrePublication.mergeTableSelection(publication.getCreationTables(), (PostgreTable) event.getElement(), event.getChecked()));
            }
        });
        listener = new DBECommandAdapter() {
            @Override
            public void onCommandChange(@NotNull DBECommand<?> command) {
                refreshSelection();
            }

            @Override
            public void onCommandUndo(@NotNull DBECommand<?> command) {
                refreshSelection();
            }

            @Override
            public void onSave() {
                refreshSelection();
            }

            @Override
            public void onReset() {
                refreshSelection();
            }
        };
        getEditorInput().getCommandContext().addCommandListener(listener);
        loadJob = new AbstractJob(PostgreMessages.dialog_create_publication_tables) {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                try {
                    List<PostgreTable> available = publication.getAvailableTables(monitor);
                    UIUtils.asyncExec(() -> {
                        if (!viewer.getTable().isDisposed()) {
                            viewer.setInput(available);
                            refreshSelection();
                        }
                    });
                    return Status.OK_STATUS;
                } catch (Exception e) {
                    return GeneralUtils.makeExceptionStatus(e);
                }
            }
        };
        loadJob.setUser(true);
        loadJob.schedule();
    }

    private void refreshSelection() {
        UIUtils.asyncExec(() -> {
            if (!viewer.getTable().isDisposed()) {
                PostgrePublication publication = getDatabaseObject();
                viewer.setCheckedElements(publication.getCreationTables().toArray());
                viewer.getTable().setEnabled(!publication.isPersisted());
                filterText.setEnabled(!publication.isPersisted());
                message.setText(publication.isAllTables() && !publication.getCreationTables().isEmpty()
                    ? PostgreMessages.dialog_create_publication_error_all_tables : PostgreMessages.dialog_create_publication_tables);
                message.getParent().layout(true, true);
            }
        });
    }

    @NotNull
    @Override
    public RefreshResult refreshPart(@Nullable Object source, boolean force) {
        refreshSelection();
        return RefreshResult.REFRESHED;
    }

    @Override
    public void setFocus() {
        filterText.setFocus();
    }

    @Override
    public void dispose() {
        if (loadJob != null) {
            loadJob.cancel();
        }
        if (listener != null) {
            getEditorInput().getCommandContext().removeCommandListener(listener);
        }
        super.dispose();
    }
}
