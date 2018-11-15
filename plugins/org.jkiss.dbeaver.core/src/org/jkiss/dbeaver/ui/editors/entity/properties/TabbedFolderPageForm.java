/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.prop.DBECommandProperty;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAdapter;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.editor.EntityEditorsRegistry;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderPage;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

/**
 * TabbedFolderPageProperties
 */
public class TabbedFolderPageForm extends TabbedFolderPage implements IRefreshablePart, ICustomActionsProvider {

    private static final Log log = Log.getLog(TabbedFolderPageForm.class);

    private static final String VALUE_KEY = "form.data.value";
    private static final String LIST_VALUE_KEY = "form.data.list.value";

    protected IWorkbenchPart part;
    protected ObjectEditorPageControl ownerControl;
    protected IDatabaseEditorInput input;
    private Font boldFont;
    private Composite propertiesGroup;
    private DBPPropertySource curPropertySource;
    private final Map<DBPPropertyDescriptor, Control> editorMap = new HashMap<>();
    private boolean activated;
    private Button saveButton;
    private Button scriptButton;
    private Button revertButton;
    private Composite curButtonsContainer;

    private transient volatile boolean isLoading;

    public TabbedFolderPageForm(IWorkbenchPart part, ObjectEditorPageControl ownerControl, IDatabaseEditorInput input) {
        this.part = part;
        this.ownerControl = ownerControl;
        this.input = input;
    }

    @Override
    public void createControl(Composite parent)
    {
        this.boldFont = UIUtils.makeBoldFont(parent.getFont());

//        ScrolledComposite scrolled = new ScrolledComposite(parent, SWT.V_SCROLL);
//        scrolled.setLayout(new GridLayout(1, false));

        propertiesGroup = new Composite(parent, SWT.NONE);
        //propertiesGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
//        scrolled.setContent(propertiesGroup);
//        scrolled.setExpandHorizontal(true);
//        scrolled.setExpandVertical(true);
//
//        scrolled.addListener( SWT.Resize, event -> {
//            int width = scrolled.getClientArea().width;
//            scrolled.setMinSize( propertiesGroup.computeSize( width, SWT.DEFAULT ) );
//        } );


        curPropertySource = input.getPropertySource();
        refreshProperties();
        input.getCommandContext().addCommandListener(new DBECommandAdapter() {
            @Override
            public void onCommandChange(DBECommand command) {
                UIUtils.asyncExec(() -> {
                    updateEditButtonsState();
                    if (command instanceof DBECommandProperty) {
                        // We need to exclude current prop from update
                        // Simple value compare on update is not enough because value can be transformed (e.g. uppercased)
                        // and it will differ from the value in edit control
                        Object propId = ((DBECommandProperty) command).getHandler().getId();
                        updateOtherPropertyValues(propId);
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

        propertiesGroup.addDisposeListener(e -> dispose());
	}

    private void updateEditButtonsState() {
        if (saveButton == null || saveButton.isDisposed()) {
            return;
        }
        boolean isDirty = input.getCommandContext().isDirty();
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
        UIUtils.dispose(boldFont);
		super.dispose();
	}

    private void refreshProperties() {
        if (curPropertySource == null) {
            return;
        }
        curPropertySource = input.getPropertySource();
        List<DBPPropertyDescriptor> allProps = filterProperties(curPropertySource.getPropertyDescriptors2());

        boolean firstInit = editorMap.isEmpty();
        if (firstInit) {
            // Prepare property lists
            List<DBPPropertyDescriptor> primaryProps = new ArrayList<>();
            List<DBPPropertyDescriptor> secondaryProps = new ArrayList<>();

            if (isEditableObject()) {
                for (DBPPropertyDescriptor prop : allProps) {
                    if (prop.getId().equals(DBConstants.PROP_ID_NAME) ||
                        prop.getId().equals(DBConstants.PROP_ID_DESCRIPTION) ||
                        prop.isEditable(curPropertySource.getEditableValue())) {
                        primaryProps.add(prop);
                    } else {
                        secondaryProps.add(prop);
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
            int colCount = 1;
            if (hasEditButtons) colCount++;
            if (hasSecondaryProps) colCount++;
            GridLayout propsLayout = new GridLayout(colCount, true);
            propertiesGroup.setLayout(propsLayout);

            Control parent = propertiesGroup;
            int editorWidth = parent.getSize().x;
            while (editorWidth == 0 && parent != null) {
                editorWidth = parent.getSize().x;
                parent = parent.getParent();
            }
            int minGroupWidth = UIUtils.getFontHeight(propertiesGroup) * 30;
            int maxGroupWidth = (editorWidth * 33) / 100; // Edit panel width max = 35%
            int buttonPanelWidth = (editorWidth / 10); // Edit panel width max = 10%
            if (maxGroupWidth < minGroupWidth) {
                // Narrow screen. Use auto-layout
                maxGroupWidth = minGroupWidth;
                buttonPanelWidth = 0;
            }

            Composite primaryGroup = new Composite(propertiesGroup, SWT.BORDER);
            primaryGroup.setLayout(new GridLayout(2, false));
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = maxGroupWidth;
            //gd.horizontalIndent = editorWidth / 10;
            primaryGroup.setLayoutData(gd);

            Composite secondaryGroup = null;
            if (hasSecondaryProps) {
                secondaryGroup = new Composite(propertiesGroup, SWT.BORDER);
                secondaryGroup.setLayout(new GridLayout(2, false));
                gd = new GridData(GridData.FILL_BOTH);
                gd.widthHint = maxGroupWidth;
                secondaryGroup.setLayoutData(gd);
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
                scriptButton = UIUtils.createPushButton(buttonsGroup, "View script", DBeaverIcons.getImage(UIIcon.SQL_SCRIPT), new SelectionAdapter() {
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
            try {
                isLoading = true;

                for (DBPPropertyDescriptor primaryProp : primaryProps) {
                    createPropertyEditor(primaryGroup, primaryProp);
                }
                if (secondaryGroup != null) {
                    for (DBPPropertyDescriptor secondaryProp : secondaryProps) {
                        createPropertyEditor(secondaryGroup, secondaryProp);
                    }
                }
            } finally {
                isLoading = false;
            }
        }

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
        disableControls = false;
        ControlEnableState blockEnableState = disableControls ? ControlEnableState.disable(propertiesGroup) : null;

        ownerControl.runService(
            LoadingJob.createService(
                new DatabaseLoadService<Map<DBPPropertyDescriptor, Object>>("Load main properties", input.getDatabaseObject().getDataSource()) {
                    @Override
                    public Map<DBPPropertyDescriptor, Object> evaluate(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
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
                    loadEditorValues(editorValues);
                    if (blockEnableState != null) {
                        blockEnableState.restore();
                    }
                })
            )
        );
    }

    private void updateOtherPropertyValues(Object excludePropId) {
        List<DBPPropertyDescriptor> allProps = filterProperties(curPropertySource.getPropertyDescriptors2());

        Map<DBPPropertyDescriptor, Object> propValues = new HashMap<>();
        for (DBPPropertyDescriptor prop : allProps) {
            if (excludePropId != null && excludePropId.equals(prop.getId())) {
                continue;
            }
            Object value = curPropertySource.getPropertyValue(null, prop.getId());
            propValues.put(prop, value);
        }
        loadEditorValues(propValues);
    }

    private boolean isEditableObject() {
        for (DBPPropertyDescriptor prop : curPropertySource.getPropertyDescriptors2()) {
            if (prop.isEditable(curPropertySource.getEditableValue()) ||
                (prop.getId().equals(DBConstants.PROP_ID_NAME) && supportsObjectRename()))
            {
                return true;
            }
        }
        return false;
    }

    private boolean supportsObjectRename() {
        return EntityEditorsRegistry.getInstance().getObjectManager(
            curPropertySource.getEditableValue().getClass(), DBEObjectRenamer.class) != null;
    }

    private void createPropertyEditor(Composite group, DBPPropertyDescriptor prop) {
        if (prop == null) {
            UIUtils.createEmptyLabel(group, 2, 1);
        } else {
            boolean editable = prop.isEditable(curPropertySource.getEditableValue()) ||
                (prop.getId().equals(DBConstants.PROP_ID_NAME) && supportsObjectRename());
            Class<?> propType = prop.getDataType();
            Object propertyValue = curPropertySource.getPropertyValue(null, prop.getId());
            if (propertyValue == null && prop instanceof ObjectPropertyDescriptor && ((ObjectPropertyDescriptor) prop).isOptional()) {
                // Do not create editor for null optional properties
                return;
            }
            Control editControl = createEditorControl(
                group,
                curPropertySource.getEditableValue(),
                prop,
                propertyValue,
                !editable);
            String propDescription = prop.getDescription();
            if (!CommonUtils.isEmpty(propDescription)) {
                editControl.setToolTipText(propDescription);
            }
            if (editControl instanceof Button) {
                // nothing
            } else {
                //boolean plainText = (CharSequence.class.isAssignableFrom(propType));
                GridData gd = (GridData) editControl.getLayoutData();
                if (gd == null) {
                    gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
                    editControl.setLayoutData(gd);
                }
                if (editControl instanceof Text || editControl instanceof Combo) {
                    gd.widthHint = Math.max(
                        UIUtils.getFontHeight(group) * 15,
                        editControl.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
                }
            }

            editorMap.put(prop, editControl);

            Control finalEditControl = editControl;

            if (finalEditControl instanceof Combo) {
                ((Combo) finalEditControl).addModifyListener(e -> updatePropertyValue(prop, ((Combo) finalEditControl).getText()));
            } else if (finalEditControl instanceof Text) {
                ((Text) finalEditControl).addModifyListener(e -> updatePropertyValue(prop, ((Text) finalEditControl).getText()));
            } else if (finalEditControl instanceof Button) {
                ((Button) finalEditControl).addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        updatePropertyValue(prop, ((Button) finalEditControl).getSelection());
                    }
                });
            }
        }
    }

    private void updatePropertyValue(DBPPropertyDescriptor prop, Object value) {
        if (!isLoading) {
            DBSObject databaseObject = input.getDatabaseObject();
            if (prop.getId().equals(DBConstants.PROP_ID_NAME) && databaseObject.isPersisted()) {
                DBEObjectRenamer renamer = EntityEditorsRegistry.getInstance().getObjectManager(curPropertySource.getEditableValue().getClass(), DBEObjectRenamer.class);
                if (renamer != null) {
                    try {
                        renamer.renameObject(input.getCommandContext(), databaseObject, CommonUtils.toString(value));
                    } catch (Throwable e) {
                        log.error("Error renaming object", e);
                    }
                }
            } else {
                Class<?> dataType = prop.getDataType();
                if (value instanceof String) {
                    value = GeneralUtils.convertString((String) value, dataType);
                }
                curPropertySource.setPropertyValue(null, prop.getId(), value);
            }
        }
    }

    /**
     * Supported editors:
     * Combo (lists)
     * Text (strings, numbers, dates)
     * Button (booleans)
     */
    public Control createEditorControl(Composite parent, Object object, DBPPropertyDescriptor property, Object value, boolean readOnly)
    {
        // List
        if (!readOnly && property instanceof IPropertyValueListProvider) {
            final IPropertyValueListProvider listProvider = (IPropertyValueListProvider) property;
            Object[] items = listProvider.getPossibleValues(object);
            if (items == null && property instanceof ObjectPropertyDescriptor && ((ObjectPropertyDescriptor) property).hasListValueProvider()) {
                // It is a list provider but it seems to be lazy and not yet initialized
                items = new Object[0];
            }
            if (items != null) {
                final String[] strings = new String[items.length];
                for (int i = 0, itemsLength = items.length; i < itemsLength; i++) {
                    strings[i] = objectValueToString(items[i]);
                }
                Combo combo = UIUtils.createLabelCombo(parent, property.getDisplayName(), SWT.BORDER | SWT.DROP_DOWN | (listProvider.allowCustomValue() ? SWT.NONE : SWT.READ_ONLY) | (readOnly ? SWT.READ_ONLY : SWT.NONE));
                combo.setItems(strings);
                combo.setText(objectValueToString(value));
                combo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING));
                return combo;
            }
        }
        Class<?> propType = property.getDataType();
        if (isTextPropertyType(propType)) {
            if (property instanceof ObjectPropertyDescriptor && ((ObjectPropertyDescriptor) property).isMultiLine()) {
                Text editor = UIUtils.createLabelText(
                    parent,
                    property.getDisplayName(),
                    objectValueToString(value),
                    SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | (readOnly ? SWT.READ_ONLY : SWT.NONE));
                GridData gd = new GridData(GridData.FILL_BOTH);
                gd.heightHint = UIUtils.getFontHeight(editor) * 4;
                editor.setLayoutData(gd);
                return editor;
            } else {
                Text text = UIUtils.createLabelText(
                    parent,
                    property.getDisplayName(),
                    objectValueToString(value),
                    SWT.BORDER | (readOnly ? SWT.READ_ONLY : SWT.NONE));
                text.setLayoutData(new GridData((BeanUtils.isNumericType(propType) ? GridData.HORIZONTAL_ALIGN_BEGINNING : GridData.FILL_HORIZONTAL) | GridData.VERTICAL_ALIGN_BEGINNING));
                return text;
            }
        } else if (BeanUtils.isBooleanType(propType)) {
            if (curButtonsContainer == null) {
                UIUtils.createEmptyLabel(parent, 1, 1);
                curButtonsContainer = new Composite(parent, SWT.NONE);
                RowLayout layout = new RowLayout(SWT.HORIZONTAL);
                curButtonsContainer.setLayout(layout);
                GridData gd = new GridData(GridData.FILL_HORIZONTAL);
                curButtonsContainer.setLayoutData(gd);
            }
            Button editor = UIUtils.createCheckbox(curButtonsContainer, property.getDisplayName(), "", CommonUtils.toBoolean(value), 1);
            if (readOnly) {
                editor.setEnabled(false);
            }
            return editor;
        } else if (!readOnly && propType.isEnum()) {
            final Object[] enumConstants = propType.getEnumConstants();
            final String[] strings = new String[enumConstants.length];
            for (int i = 0, itemsLength = enumConstants.length; i < itemsLength; i++) {
                strings[i] = ((Enum)enumConstants[i]).name();
            }
            Combo combo = UIUtils.createLabelCombo(
                parent,
                property.getDisplayName(),
                SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY | (readOnly ? SWT.READ_ONLY : SWT.NONE));
            combo.setItems(strings);
            combo.setText(objectValueToString(value));
            combo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING));
            return combo;
        } else if (DBSObject.class.isAssignableFrom(propType) || (property instanceof ObjectPropertyDescriptor && ((ObjectPropertyDescriptor)property).isLinkPossible())) {
            UIUtils.createControlLabel(
                parent,
                property.getDisplayName());
            Composite linkPH = new Composite(parent, SWT.NONE);
            {
                linkPH.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            }
            GridLayout layout = new GridLayout(1, false);
            layout.marginHeight = 1;
            linkPH.setLayout(layout);
            Link link = new Link(linkPH, SWT.NONE);
            link.setText(getLinktitle(value));
            link.setData(value);
            link.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DBSObject object = (DBSObject) link.getData();
                    if (object != null) {
                        NavigatorHandlerObjectOpen.openEntityEditor(object);
                    }
                }
            });
            link.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            return link;
        } else {
            return UIUtils.createLabelText(
                parent,
                property.getDisplayName(),
                objectValueToString(value),
                SWT.BORDER | SWT.READ_ONLY);
        }
    }

    private String getLinktitle(Object value) {
        return value == null ? "N/A" : "<a>" + objectValueToString(value) + "</a>";
    }

    private void loadEditorValues(Map<DBPPropertyDescriptor, Object> editorValues) {
        try {
            isLoading = true;
            if (curPropertySource != null) {
                Object object = curPropertySource.getEditableValue();
                for (Map.Entry<DBPPropertyDescriptor, Object> prop : editorValues.entrySet()) {
                    setEditorValue(object, prop.getKey(), prop.getValue());
                }
            }
        } finally {
            isLoading = false;
        }
    }

    public void setEditorValue(Object object, DBPPropertyDescriptor property, Object value)
    {
        Control editorControl = editorMap.get(property);
        Class<?> propertyType = property.getDataType();
        // List
        String stringValue = objectValueToString(value);
        if (editorControl instanceof Combo) {
            Combo combo = (Combo) editorControl;
            if (property instanceof IPropertyValueListProvider) {
                final IPropertyValueListProvider listProvider = (IPropertyValueListProvider) property;
                final Object[] items = listProvider.getPossibleValues(object);
                final Object[] oldItems = (Object[]) combo.getData(LIST_VALUE_KEY);
                combo.setData(LIST_VALUE_KEY, items);
                if (items != null) {
                    if (oldItems == null || !Arrays.equals(items, oldItems)) {
                        final String[] strings = new String[items.length];
                        for (int i = 0, itemsLength = items.length; i < itemsLength; i++) {
                            strings[i] = items[i] instanceof DBPNamedObject ? ((DBPNamedObject) items[i]).getName() : CommonUtils.toString(items[i]);
                        }
                        combo.setItems(strings);
                    }
                    combo.setText(stringValue);
                }
            } else if (propertyType.isEnum()) {
                if (combo.getItemCount() == 0) {
                    // Do not refresh enum values - they are static
                    final Object[] enumConstants = propertyType.getEnumConstants();
                    final String[] strings = new String[enumConstants.length];
                    for (int i = 0, itemsLength = enumConstants.length; i < itemsLength; i++) {
                        strings[i] = ((Enum) enumConstants[i]).name();
                    }
                    combo.setItems(strings);
                }
                combo.setText(stringValue);
            }
        } else {
            if (editorControl instanceof Text) {
                Text text = (Text) editorControl;
                if (!CommonUtils.equalObjects(text.getText(), stringValue)) {
                    text.setText(stringValue);
                }
            } else if (editorControl instanceof Button) {
                ((Button) editorControl).setSelection(CommonUtils.toBoolean(value));
            } else if (editorControl instanceof Link) {
                Link link = (Link)editorControl;
                link.setData(value);
                link.setText(getLinktitle(value));
            }
        }
    }

    private static String objectValueToString(Object value) {
        if (value instanceof DBPNamedObject) {
            return ((DBPNamedObject) value).getName();
        } else if (value instanceof Enum) {
            return ((Enum) value).name();
        } else {
            return DBValueFormatting.getDefaultValueDisplayString(value, DBDDisplayFormat.EDIT);
        }
    }

    private static boolean isTextPropertyType(Class<?> propertyType) {
        return propertyType == null || CharSequence.class.isAssignableFrom(propertyType) || BeanUtils.isNumericType(propertyType);
    }

    private List<DBPPropertyDescriptor> filterProperties(DBPPropertyDescriptor[] props) {
        List<DBPPropertyDescriptor> result = new ArrayList<>();
        for (DBPPropertyDescriptor prop : props) {
            String category = prop.getCategory();
            if (!CommonUtils.isEmpty(category)) {
                // Keep only basic properties
                continue;
            }
            result.add(prop);
        }
        return result;
    }

    @Override
    public void refreshPart(Object source, boolean force) {
        // Refresh props only on force refresh (manual)
        if (force) {
            refreshProperties();
            updateEditButtonsState();
        }
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