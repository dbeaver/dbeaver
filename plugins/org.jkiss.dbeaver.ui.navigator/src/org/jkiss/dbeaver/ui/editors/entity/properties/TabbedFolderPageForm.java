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
package org.jkiss.dbeaver.ui.editors.entity.properties;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBIcon;
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
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.CustomFormEditor;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderPage;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TabbedFolderPageProperties
 */
public class TabbedFolderPageForm extends TabbedFolderPage implements IRefreshablePart, ICustomActionsProvider {

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
                if (linkData instanceof DBSObject) {
                    NavigatorHandlerObjectOpen.openEntityEditor((DBSObject) linkData);
                }
            }
        };
    }

    @Override
    public void createControl(Composite parent)
    {
//        ScrolledComposite scrolled = new ScrolledComposite(parent, SWT.V_SCROLL);
//        scrolled.setLayout(new GridLayout(1, false));

        propertiesGroup = new Composite(parent, SWT.NONE);

        //CSSUtils.setCSSClass(propertiesGroup, DBStyles.COLORED_BY_CONNECTION_TYPE);

        curPropertySource = input.getPropertySource();

        DBECommandContext commandContext = input.getCommandContext();
        if (commandContext != null) {
            commandContext.addCommandListener(new DBECommandAdapter() {
                @Override
                public void onCommandChange(DBECommand<?> command) {
                    UIUtils.asyncExec(() -> {
                        updateEditButtonsState();
                        if (command instanceof DBECommandProperty) {
                            // We need to exclude current prop from update
                            // Simple value compare on update is not enough because value can be transformed (e.g. uppercased)
                            // and it will differ from the value in edit control
                            Object propId = ((DBECommandProperty<?>) command).getHandler().getId();
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

    private void refreshProperties() {
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
            // Prepare property lists
            List<DBPPropertyDescriptor> primaryProps = new ArrayList<>();
            List<DBPPropertyDescriptor> secondaryProps = new ArrayList<>();
            List<DBPPropertyDescriptor> specificProps = new ArrayList<>();

            if (formEditor.isEditableObject()) {
                for (DBPPropertyDescriptor prop : allProps) {
                    if (prop.getId().equals(DBConstants.PROP_ID_NAME) ||
                        prop.getId().equals(DBConstants.PROP_ID_DESCRIPTION) ||
                        prop.isEditable(curPropertySource.getEditableValue())) {
                        primaryProps.add(prop);
                    } else {
                        if (prop instanceof ObjectPropertyDescriptor && ((ObjectPropertyDescriptor) prop).isSpecific()) {
                            specificProps.add(prop);
                        } else {
                            secondaryProps.add(prop);
                        }
                    }
                }
                if (primaryProps.isEmpty()) {
                    primaryProps.addAll(secondaryProps);
                    secondaryProps.clear();
                }
            } else {
                primaryProps.addAll(allProps);
            }

            // Create edit panels
            boolean hasEditButtons = false;//isEditableObject();
            boolean hasSecondaryProps = !secondaryProps.isEmpty();
            boolean hasSpecificProps = !specificProps.isEmpty();
            int colCount = 1;
            if (hasEditButtons) colCount++;
            if (hasSecondaryProps) colCount++;
            if (hasSpecificProps) colCount++;
            GridLayout propsLayout = new GridLayout(colCount, true);
            propertiesGroup.setLayout(propsLayout);

            Control parent = propertiesGroup;
            int editorWidth = parent.getSize().x;
            while (editorWidth == 0 && parent != null) {
                editorWidth = parent.getSize().x;
                parent = parent.getParent();
            }

            Composite primaryGroup = new Composite(propertiesGroup, SWT.NONE);
            //CSSUtils.setCSSClass(primaryGroup, DBStyles.COLORED_BY_CONNECTION_TYPE);
            GridLayout primaryLayout = new GridLayout(2, false);
            primaryGroup.setLayout(primaryLayout);

            editorWidth -= (2 * primaryLayout.marginWidth) + ((colCount - 1) * primaryLayout.horizontalSpacing); // Minus margins and borders
            int minGroupWidth = UIUtils.getFontHeight(propertiesGroup) * 30;
            int maxGroupWidth = (editorWidth * (100 / colCount)) / 100; // Edit panel width max = 35%
            int buttonPanelWidth = (editorWidth / 10); // Edit panel width max = 10%
            if (maxGroupWidth < minGroupWidth) {
                // Narrow screen. Use auto-layout
                minGroupWidth = maxGroupWidth;
                buttonPanelWidth = 0;
            }

            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = maxGroupWidth;
            //gd.horizontalIndent = editorWidth / 10;
            primaryGroup.setLayoutData(gd);

            Composite secondaryGroup = null;
            if (hasSecondaryProps) {
                secondaryGroup = new Composite(propertiesGroup, SWT.NONE);
                secondaryGroup.setLayout(new GridLayout(2, false));
                //CSSUtils.setCSSClass(secondaryGroup, DBStyles.COLORED_BY_CONNECTION_TYPE);
                gd = new GridData(GridData.FILL_BOTH);
                gd.widthHint = maxGroupWidth;
                secondaryGroup.setLayoutData(gd);
            }

            Composite specificGroup = null;
            if (hasSpecificProps) {
                specificGroup = new Composite(propertiesGroup, SWT.NONE);
                specificGroup.setLayout(new GridLayout(2, false));
                //CSSUtils.setCSSClass(secondaryGroup, DBStyles.COLORED_BY_CONNECTION_TYPE);
                gd = new GridData(GridData.FILL_BOTH);
                gd.widthHint = maxGroupWidth;
                specificGroup.setLayoutData(gd);
            }

            if (hasEditButtons) {
                Composite buttonsGroup = new Composite(propertiesGroup, SWT.NONE);
                gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
                gd.widthHint = buttonPanelWidth;
                buttonsGroup.setLayoutData(gd);
                RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
                rowLayout.pack = true;
                rowLayout.fill = true;
                buttonsGroup.setLayout(rowLayout);
                saveButton = UIUtils.createPushButton(buttonsGroup, "Save", DBeaverIcons.getImage(UIIcon.SAVE), new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        ActionUtils.runCommand(IWorkbenchCommandConstants.FILE_SAVE, part.getSite());
                    }
                });
                scriptButton = UIUtils.createPushButton(buttonsGroup, "View script", DBeaverIcons.getImage(DBIcon.TREE_SCRIPT), new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        showAlterScript();
                    }
                });
                revertButton = UIUtils.createPushButton(buttonsGroup, "Revert", DBeaverIcons.getImage(UIIcon.REVERT), new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        ActionUtils.runCommand(IWorkbenchCommandConstants.FILE_REVERT, part.getSite());
                    }
                });
                saveButton.setEnabled(false);
                scriptButton.setEnabled(false);
                revertButton.setEnabled(false);
            }

            if (editorWidth > 1000) {
                Composite panelTail = UIUtils.createPlaceholder(propertiesGroup, 1);
                panelTail.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                propsLayout.numColumns++;
            }

            // Create edit forms
            for (DBPPropertyDescriptor primaryProp : primaryProps) {
                formEditor.createPropertyEditor(primaryGroup, primaryProp);
            }
            if (secondaryGroup != null) {
                for (DBPPropertyDescriptor secondaryProp : secondaryProps) {
                    formEditor.createPropertyEditor(secondaryGroup, secondaryProp);
                }
            }
            if (specificGroup != null) {
                for (DBPPropertyDescriptor specProps : specificProps) {
                    formEditor.createPropertyEditor(specificGroup, specProps);
                }
            }
            if (!firstInit) {
                propertiesGroup.layout(true, true);
            }
        }

        UIUtils.installAndUpdateMainFont(propertiesGroup);
        refreshPropertyValues(allProps, firstInit);
    }

    private void showAlterScript() {
        EntityEditor ownerEditor = getOwnerEditor();
        if (ownerEditor != null) {
            ownerEditor.showChanges(false);
        }
    }

    private EntityEditor getOwnerEditor() {
        IWorkbenchPartSite site = part.getSite();
        if (site instanceof MultiPageEditorSite) {
            MultiPageEditorPart mainEditor = ((MultiPageEditorSite) site).getMultiPageEditor();
            if (mainEditor instanceof EntityEditor) {
                return ((EntityEditor) mainEditor);
            }
        }
        return null;
    }

    private void refreshPropertyValues(List<DBPPropertyDescriptor> allProps, boolean disableControls) {
        DBSObject databaseObject = input.getDatabaseObject();
        if (databaseObject == null) {
            // Disposed
            return;
        }

        disableControls = false;
        ControlEnableState blockEnableState = disableControls ? ControlEnableState.disable(propertiesGroup) : null;

        ownerControl.runService(
            LoadingJob.createService(
                new DatabaseLoadService<Map<DBPPropertyDescriptor, Object>>("Load main properties", databaseObject.getDataSource()) {
                    @Override
                    public Map<DBPPropertyDescriptor, Object> evaluate(DBRProgressMonitor monitor) {
                        DBPPropertySource propertySource = TabbedFolderPageForm.this.curPropertySource;
                        monitor.beginTask("Load '" + DBValueFormatting.getDefaultValueDisplayString(propertySource.getEditableValue(), DBDDisplayFormat.UI) + "' properties", allProps.size());
                        Map<DBPPropertyDescriptor, Object> propValues = new HashMap<>();
                        for (DBPPropertyDescriptor prop : allProps) {
                            if (monitor.isCanceled()) {
                                break;
                            }
                            Object value = propertySource.getPropertyValue(monitor, prop.getId());
                            propValues.put(prop, value);
                            monitor.worked(1);
                        }
                        monitor.done();
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
            )
        );
    }

    @Override
    public RefreshResult refreshPart(Object source, boolean force) {
        // Refresh props only on force refresh (manual)
        if (force) {
            refreshProperties();
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