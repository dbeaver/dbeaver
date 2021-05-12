/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.contentassist.StringContentProposalProvider;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.*;

/**
 * CustomFormEditor
 */
public class CustomFormEditor {
    private static final Log log = Log.getLog(CustomFormEditor.class);

    private static final String VALUE_KEY = "form.data.value";
    private static final String LIST_VALUE_KEY = "form.data.list.value";

    private final Map<DBPPropertyDescriptor, Control> editorMap = new HashMap<>();
    @NotNull
    private final DBSObject databaseObject;
    @Nullable
    private final DBECommandContext commandContext;
    @NotNull
    private final DBPPropertySource propertySource;

    private Composite curButtonsContainer;
    private transient boolean isLoading;

    ///////////////////////////////////////////////
    //

    public CustomFormEditor(@NotNull DBSObject databaseObject, @Nullable DBECommandContext commandContext, @NotNull DBPPropertySource propertySource) {
        this.databaseObject = databaseObject;
        this.commandContext = commandContext;
        this.propertySource = propertySource;
    }

    protected void openObjectLink(Object linkData) {
        if (linkData != null) {
            UIUtils.openWebBrowser(CommonUtils.toString(linkData));
        }
    }

    ////////////////////////////////////////////////
    //

    public void updateOtherPropertyValues(Object excludePropId) {
        List<DBPPropertyDescriptor> allProps = filterProperties(propertySource.getProperties());

        Map<DBPPropertyDescriptor, Object> propValues = new HashMap<>();
        for (DBPPropertyDescriptor prop : allProps) {
            if (excludePropId != null && excludePropId.equals(prop.getId())) {
                continue;
            }
            Object value = propertySource.getPropertyValue(null, prop.getId());
            propValues.put(prop, value);
        }
        loadEditorValues(propValues);
    }

    public boolean isEditableObject() {
        for (DBPPropertyDescriptor prop : propertySource.getProperties()) {
            if (prop.isEditable(propertySource.getEditableValue()) ||
                (prop.getId().equals(DBConstants.PROP_ID_NAME) && supportsObjectRename()))
            {
                return true;
            }
        }
        return false;
    }

    private boolean supportsObjectRename() {
        return DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(
            propertySource.getEditableValue().getClass(), DBEObjectRenamer.class) != null;
    }

    public void createPropertyEditor(Composite group, DBPPropertyDescriptor prop) {

        isLoading = true;

        try {
            boolean isReadOnlyCon = databaseObject == null || DBUtils.isReadOnly(databaseObject);
            if (prop == null) {
                UIUtils.createEmptyLabel(group, 2, 1);
            } else {
                boolean editable = !isReadOnlyCon && (prop.isEditable(propertySource.getEditableValue()) ||
                    (prop.getId().equals(DBConstants.PROP_ID_NAME) && supportsObjectRename()));
                Class<?> propType = prop.getDataType();
                Object propertyValue = propertySource.getPropertyValue(null, prop.getId());
                if (propertyValue == null && prop instanceof ObjectPropertyDescriptor && ((ObjectPropertyDescriptor) prop).isOptional()) {
                    // Do not create editor for null optional properties
                    return;
                }
                Control editControl = createEditorControl(
                    group,
                    propertySource.getEditableValue(),
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
                    if ((finalEditControl.getStyle() & SWT.READ_ONLY) == SWT.READ_ONLY) {
                        ((Combo) finalEditControl).addSelectionListener(new SelectionAdapter() {
                            @Override
                            public void widgetSelected(SelectionEvent e) {
                                updatePropertyValue(prop, ((Combo) finalEditControl).getText());
                            }
                        });
                    } else {
                        ((Combo) finalEditControl).addModifyListener(e -> {
                            try {
                                updatePropertyValue(prop, ((Combo) finalEditControl).getText());
                            } catch (Exception ex) {
                                log.debug("Error setting value from combo: " + ex.getMessage());
                            }
                        });
                    }
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
        } finally {
            isLoading = false;
        }
    }

    private void updatePropertyValue(DBPPropertyDescriptor prop, Object value) {
        if (!isLoading) {
            if (prop.getId().equals(DBConstants.PROP_ID_NAME) && databaseObject.isPersisted()) {
                DBEObjectRenamer renamer = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(propertySource.getEditableValue().getClass(), DBEObjectRenamer.class);
                if (commandContext != null && renamer != null) {
                    try {
                        Map<String, Object> options = new LinkedHashMap<>();
                        options.put(DBEObjectManager.OPTION_UI_SOURCE, this);
                        renamer.renameObject(commandContext, databaseObject, options, CommonUtils.toString(UIUtils.normalizePropertyValue(value)));
                    } catch (Throwable e) {
                        log.error("Error renaming object", e);
                    }
                }
            } else {
                Class<?> dataType = prop.getDataType();
                if (value instanceof String) {
                    value = GeneralUtils.convertString((String) UIUtils.normalizePropertyValue(value), dataType);
                }
                Object oldPropValue = propertySource.getPropertyValue(null, prop.getId());
                propertySource.setPropertyValue(null, prop.getId(), value);
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
                Combo combo = UIUtils.createLabelCombo(
                    parent,
                    property.getDisplayName(),
                    SWT.BORDER | SWT.DROP_DOWN |
                        (listProvider.allowCustomValue() ? SWT.NONE : SWT.READ_ONLY) |
                        (readOnly ? SWT.READ_ONLY : SWT.NONE));
                combo.setItems(strings);
                combo.setText(objectValueToString(value));
                combo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING));

                if ((combo.getStyle() & SWT.READ_ONLY) == 0) {
                    StringContentProposalProvider proposalProvider = new StringContentProposalProvider(strings);
                    ContentAssistUtils.installContentProposal(combo, new ComboContentAdapter(), proposalProvider);
                }

                return combo;
            }
        }
        Class<?> propType = property.getDataType();
        if (DBSObject.class.isAssignableFrom(propType) || isLinkProperty(property)) {
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
                    openObjectLink(link.getData());
                }
            });
            link.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            return link;
        } else if (isTextPropertyType(propType)) {
            if (property instanceof ObjectPropertyDescriptor && ((ObjectPropertyDescriptor) property).getLength() == PropertyLength.MULTILINE) {
                Label label = UIUtils.createControlLabel(parent, property.getDisplayName());
                label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
                Text editor = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | (readOnly ? SWT.READ_ONLY : SWT.NONE));

                editor.setText(objectValueToString(value));
                GridData gd = new GridData(GridData.FILL_BOTH);
                // Make multline editor at least two lines height
                gd.heightHint = (UIUtils.getTextHeight(editor) + editor.getBorderWidth()) * 2;
                editor.setLayoutData(gd);
                return editor;
            } else {
                Text text = UIUtils.createLabelText(
                    parent,
                    property.getDisplayName(),
                    objectValueToString(value),
                    SWT.BORDER |
                        (readOnly ? SWT.READ_ONLY : SWT.NONE) |
                        (property instanceof ObjectPropertyDescriptor && ((ObjectPropertyDescriptor) property).isPassword() ? SWT.PASSWORD : SWT.NONE));
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
        } else {
            return UIUtils.createLabelText(
                parent,
                property.getDisplayName(),
                objectValueToString(value),
                SWT.BORDER | SWT.READ_ONLY);
        }
    }

    private static boolean isLinkProperty(DBPPropertyDescriptor property) {
        return property instanceof ObjectPropertyDescriptor &&
            (((ObjectPropertyDescriptor)property).isLinkPossible() || ((ObjectPropertyDescriptor)property).isHref());
    }

    private String getLinktitle(Object value) {
        return value == null ? "N/A" : "<a>" + objectValueToString(value) + "</a>";
    }

    public void loadEditorValues(Map<DBPPropertyDescriptor, Object> editorValues) {
        try {
            isLoading = true;
            if (propertySource != null) {
                Object object = propertySource.getEditableValue();
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
                        strings[i] = ((Enum<?>) enumConstants[i]).name();
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
        if (value instanceof DBPQualifiedObject) {
            return ((DBPQualifiedObject) value).getFullyQualifiedName(DBPEvaluationContext.UI);
        } if (value instanceof DBPNamedObject) {
            return ((DBPNamedObject) value).getName();
        } else if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        } else {
            return DBValueFormatting.getDefaultValueDisplayString(value, DBDDisplayFormat.EDIT);
        }
    }

    private static boolean isTextPropertyType(Class<?> propertyType) {
        return propertyType == null || CharSequence.class.isAssignableFrom(propertyType) ||
            (propertyType.getComponentType() != null && CharSequence.class.isAssignableFrom(propertyType.getComponentType())) ||
            BeanUtils.isNumericType(propertyType);
    }

    public List<DBPPropertyDescriptor> filterProperties(DBPPropertyDescriptor[] props) {
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


    public boolean hasEditors() {
        return !editorMap.isEmpty();
    }
}
