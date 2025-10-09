/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.entity.properties;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.widgets.CompositeFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.forms.widgets.ColumnLayout;
import org.eclipse.ui.forms.widgets.ColumnLayoutData;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.prop.DBECommandProperty;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAdapter;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.ui.ICustomActionsProvider;
import org.jkiss.dbeaver.ui.IRefreshablePart;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomFormEditor;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderPage;
import org.jkiss.dbeaver.ui.css.CSSUtils;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TabbedFolderPageProperties
 */
public class TabbedFolderPageForm extends TabbedFolderPage implements IRefreshablePart, ICustomActionsProvider {

    private static final Log log = Log.getLog(TabbedFolderPageForm.class);
    private final IWorkbenchPart part;
    private final IDatabaseEditorInput input;

    private final ObjectEditorPageControl ownerControl;
    private final CustomFormEditor formEditor;
    private Composite propertiesGroup;
    private DBPPropertySource curPropertySource;

    private boolean activated;
    private Button saveButton;
    private Button scriptButton;
    private Button revertButton;
    private transient boolean lastPersistedState;


    TabbedFolderPageForm(IWorkbenchPart part, ObjectEditorPageControl ownerControl, IDatabaseEditorInput input) {
        this.part = part;
        this.ownerControl = ownerControl;
        this.input = input;
        this.formEditor = new CustomFormEditor(input.getDatabaseObject(), input.getCommandContext(), input.getPropertySource()) {

            @Override
            protected void openObjectLink(Object linkData) {
                if (linkData instanceof DBSObject dbsObject) {
                    NavigatorHandlerObjectOpen.openEntityEditor(dbsObject);
                }
            }
        };
    }

    @Override
    public void createControl(Composite parent) {
        ScrolledComposite propertiesGroupHost = UIUtils.createScrolledComposite(parent, SWT.V_SCROLL);
        CSSUtils.markConnectionTypeColor(propertiesGroupHost);

        propertiesGroup = new Composite(propertiesGroupHost, SWT.NONE);
        UIUtils.configureScrolledComposite(propertiesGroupHost, propertiesGroup);

        curPropertySource = input.getPropertySource();

        DBECommandContext commandContext = input.getCommandContext();
        if (commandContext != null) {
            commandContext.addCommandListener(new DBECommandAdapter() {
                @Override
                public void onCommandChange(DBECommand<?> command) {
                    UIUtils.asyncExec(() -> {
                        updateEditButtonsState();
                        if (command instanceof DBECommandProperty<?> cp) {
                            // We need to exclude current prop from update
                            // Simple value compare on update is not enough because value can be transformed (e.g. uppercased)
                            // and it will differ from the value in edit control
                            Object propId = cp.getHandler().getId();
                            formEditor.updateOtherPropertyValues(propId);
                        }
                    });
                }

                @Override
                public void onSave() {
                    UIUtils.asyncExec(() -> updateEditButtonsState());
                }

                @Override
                public void onReset() {
                    UIUtils.asyncExec(() -> {
                        refreshProperties();
                        updateEditButtonsState();
                    });
                }
            });
        }

        propertiesGroup.addDisposeListener(e -> dispose());

        refreshProperties();
    }

    private void updateEditButtonsState() {
        if (saveButton == null || saveButton.isDisposed()) {
            return;
        }
        DBECommandContext commandContext = input.getCommandContext();
        boolean isDirty = commandContext != null && commandContext.isDirty();
        saveButton.setEnabled(isDirty);
        revertButton.setEnabled(isDirty);
        scriptButton.setEnabled(isDirty);
    }

    @Override
    public void setFocus() {
        propertiesGroup.setFocus();
    }

    @Override
    public void dispose() {
        if (curPropertySource != null && curPropertySource.getEditableValue() instanceof DBSObject) {
            curPropertySource = null;
        }
        super.dispose();
    }

    private void refreshProperties(){
        refreshProperties(null);
    }

    private void refreshProperties(@Nullable Runnable afterRefresh) {
        if (curPropertySource == null) {
            return;
        }
        curPropertySource = input.getPropertySource();
        List<DBPPropertyDescriptor> allProps = formEditor.filterProperties(curPropertySource.getProperties());

        DBSObject databaseObject = input.getDatabaseObject();
        if (databaseObject == null) {
            return;
        }
        boolean objectPersisted = databaseObject.isPersisted();
        boolean objectStateChanged = objectPersisted != lastPersistedState;
        lastPersistedState = objectPersisted;

        boolean firstInit = !formEditor.hasEditors();
        if (firstInit || objectStateChanged) {
            if (!firstInit) {
                // Dispose old editor
                UIUtils.disposeChildControls(propertiesGroup);
                formEditor.clearEditors();
            }

            if (firstInit) {
                // Prepare property lists
                ColumnLayout layout = new ColumnLayout();
                layout.minNumColumns = 1;
                layout.maxNumColumns = 3;
                layout.horizontalSpacing = 10;
                propertiesGroup.setLayout(layout);
                propertiesGroup.addControlListener(ControlListener.controlResizedAdapter(e -> layoutProperties()));
            }

            List<DBPPropertyDescriptor> sortedProps = new ArrayList<>(allProps);
            sortedProps.sort(Comparator
                .comparingInt((DBPPropertyDescriptor prop) -> getPropertyCategory(prop, curPropertySource)));

            // Create edit forms
            for (DBPPropertyDescriptor prop : sortedProps) {
                var placeholder = CompositeFactory.newComposite(SWT.NONE)
                    .layoutData(new ColumnLayoutData())
                    .layout(GridLayoutFactory.fillDefaults().numColumns(2).create())
                    .create(propertiesGroup);

                formEditor.createPropertyEditor(placeholder, prop);

                if (placeholder.getChildren().length == 0) {
                    placeholder.dispose();
                }
            }

            layoutProperties();
        }
        for (Control x : propertiesGroup.getChildren()) {
            CSSUtils.markConnectionTypeColor(x);
        }

        UIUtils.installAndUpdateMainFont(propertiesGroup);
        refreshPropertyValues(allProps, firstInit, afterRefresh);
    }

    private static int getPropertyCategory(@NotNull DBPPropertyDescriptor property, @NotNull DBPPropertySource source) {
        if (property.getId().equals(DBConstants.PROP_ID_NAME) || property.getId().equals(DBConstants.PROP_ID_DESCRIPTION)) {
            return 1;
        } else if (property.isEditable(source.getEditableValue())) {
            return 10;
        } else if (!(property instanceof ObjectPropertyDescriptor opd) || !opd.isSpecific()) {
            return 100;
        } else {
            return 1000;
        }
    }

    private void layoutProperties() {
        propertiesGroup.layout(true, true);
        layoutPropertyColumns(propertiesGroup);
    }

    private static void layoutPropertyColumns(@NotNull Composite composite) {
        Collection<List<Composite>> columns = Stream.of(composite.getChildren())
            .map(Composite.class::cast)
            .collect(Collectors.groupingBy(child -> child.getLocation().x))
            .values();

        boolean layout = false;
        for (List<Composite> column : columns) {
            int widthHint = computePropertyColumnWidth(column);
            if (widthHint > 0) {
                layout |= layoutPropertyColumn(column, widthHint);
            }
        }

        if (layout) {
            composite.layout(true, true);
        }
    }

    private static int computePropertyColumnWidth(@NotNull List<Composite> rows) {
        int widthHint = 0;
        for (Composite row : rows) {
            Control key = row.getChildren()[0];
            Rectangle bounds = key.getBounds();
            widthHint = Math.max(widthHint, key.computeSize(bounds.width, bounds.height).x);
        }
        return widthHint;
    }

    private static boolean layoutPropertyColumn(@NotNull Collection<Composite> rows, int widthHint) {
        boolean layout = false;
        for (Composite row : rows) {
            Control key = row.getChildren()[0];
            GridData gd;
            if (key.getLayoutData() instanceof GridData data) {
                gd = data;
            } else {
                gd = new GridData();
                key.setLayoutData(gd);
            }
            if (gd.widthHint != widthHint) {
                gd.widthHint = widthHint;
                layout = true;
            }
        }
        return layout;
    }

    private void showAlterScript() {
        EntityEditor ownerEditor = getOwnerEditor();
        if (ownerEditor != null) {
            ownerEditor.showChanges(false);
        }
    }

    private EntityEditor getOwnerEditor() {
        IWorkbenchPartSite site = part.getSite();
        if (site instanceof MultiPageEditorSite mpe) {
            MultiPageEditorPart mainEditor = mpe.getMultiPageEditor();
            if (mainEditor instanceof EntityEditor ee) {
                return ee;
            }
        }
        return null;
    }

    private void refreshPropertyValues(List<DBPPropertyDescriptor> allProps, boolean disableControls, Runnable afterRefresh) {
        DBSObject databaseObject = input.getDatabaseObject();
        if (databaseObject == null) {
            // Disposed
            return;
        }

        disableControls = false;
        ControlEnableState blockEnableState = disableControls ? ControlEnableState.disable(propertiesGroup) : null;

        DBPPropertySource propertySource = TabbedFolderPageForm.this.curPropertySource;
        LoadingJob<Map<DBPPropertyDescriptor, Object>> service = LoadingJob.createService(
            new DatabaseLoadService<>(
                "Load '" + DBValueFormatting.getDefaultValueDisplayString(
                    propertySource.getEditableValue(), DBDDisplayFormat.UI) + "' properties",
                databaseObject.getDataSource()) {
                @Override
                public Map<DBPPropertyDescriptor, Object> evaluate(DBRProgressMonitor monitor) {
                    Map<DBPPropertyDescriptor, Object> propValues = new HashMap<>();
                    for (DBPPropertyDescriptor prop : allProps) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        Object value = propertySource.getPropertyValue(monitor, prop.getId());
                        propValues.put(prop, value);
                    }
                    return propValues;
                }
            },
            ownerControl.createDefaultLoadVisualizer(editorValues -> {
                if (ownerControl.isDisposed()) {
                    return;
                }
                formEditor.loadEditorValues(editorValues);
                if (blockEnableState != null) {
                    blockEnableState.restore();
                }
            })
        );
        service.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                try {
                    if (afterRefresh != null) {
                        UIUtils.asyncExec(afterRefresh);
                    }
                } catch (Exception e) {
                    log.warn("Exception after refreshing in TabbedFolderPageForm", e);
                }
            }
        });
        ownerControl.runService(service);
    }

    @Override
    public RefreshResult refreshPart(Object source, boolean force) {
        return refreshPart(force, null);
    }

    public RefreshResult refreshPart(boolean force, @Nullable Runnable afterRefresh) {
        // Refresh props only on force refresh (manual)
        if (force) {
            refreshProperties(afterRefresh);
            updateEditButtonsState();
            return RefreshResult.REFRESHED;
        }
        return RefreshResult.IGNORED;
    }

    @Override
    public void aboutToBeShown() {
        if (!activated) {
            activated = true;
        }
    }

    @Override
    public void fillCustomActions(IContributionManager contributionManager) {
/*
        contributionManager.add(new Action(isAttached() ? "Detach properties to top panel" : "Move properties to tab", DBeaverIcons.getImageDescriptor(UIIcon.ASTERISK)) {
            @Override
            public void run() {
                detachPropertiesPanel();
            }
        });
        if (part != null) {
            DatabaseEditorUtils.contributeStandardEditorActions(part.getSite(), contributionManager);
        }
*/
    }

}