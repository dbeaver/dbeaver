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
package org.jkiss.dbeaver.ui.properties;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.IPropertyValueValidator;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.*;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;

import java.time.LocalDateTime;

/**
 * PropertyEditorUtils
 */
public class PropertyEditorUtils {

    private static final Log log = Log.getLog(PropertyEditorUtils.class);

    public static CellEditor createPropertyEditor(final IServiceLocator serviceLocator, Composite parent, DBPPropertySource source, DBPPropertyDescriptor property, int style)
    {
        if (source == null) {
            return null;
        }
        final Object object = source.getEditableValue();
        if (!property.isEditable(object)) {
            return null;
        }
        CellEditor cellEditor = createCellEditor(parent, object, property, style);
        if (cellEditor != null) {
            final Control editorControl = cellEditor.getControl();
            UIUtils.addDefaultEditActionsSupport(serviceLocator, editorControl);
        }
        return cellEditor;
    }

    public static CellEditor createCellEditor(Composite parent, Object object, DBPPropertyDescriptor property, int style)
    {
        boolean isPropertySheet = (style & SWT.SHEET) != 0;
        style &= ~SWT.SHEET;
        // List
        if (property instanceof IPropertyValueListProvider) {
            final IPropertyValueListProvider listProvider = (IPropertyValueListProvider) property;
            final Object[] items = listProvider.getPossibleValues(object);
            if (items != null) {
                String[] strings = new String[items.length];
                for (int i = 0, itemsLength = items.length; i < itemsLength; i++) {
                    strings[i] = items[i] instanceof DBPNamedObject ? ((DBPNamedObject)items[i]).getName() : CommonUtils.toString(items[i]);
                }
                if (!property.isRequired()) {
                    // Add null value
                    strings = ArrayUtils.insertArea(String.class, strings, 0, new Object[] { "" });
                }
                final CustomComboBoxCellEditor editor = new CustomComboBoxCellEditor(
                    parent,
                    strings,
                    SWT.DROP_DOWN | (listProvider.allowCustomValue() ? SWT.NONE : SWT.READ_ONLY));
                return editor;
            }
        }
        Class<?> propertyType = property.getDataType();
        if (propertyType == null || CharSequence.class.isAssignableFrom(propertyType)) {
            if (property instanceof ObjectPropertyDescriptor && ((ObjectPropertyDescriptor) property).getLength() == PropertyLength.MULTILINE) {
                AdvancedTextCellEditor editor = new AdvancedTextCellEditor(parent);
                setValidator(editor, property, object);
                return editor;
            } else {
                CustomTextCellEditor editor = new CustomTextCellEditor(parent, SWT.SINGLE | ((style & SWT.PASSWORD) != 0 ? SWT.PASSWORD : SWT.NONE));
                setValidator(editor, property, object);
                return editor;
            }
        } else if (BeanUtils.isNumericType(propertyType)) {
            CustomNumberCellEditor editor = new CustomNumberCellEditor(parent, propertyType);
            setValidator(editor, property, object);
            return editor;
        } else if (BeanUtils.isBooleanType(propertyType)) {
            if (isPropertySheet) {
                return new CustomComboBoxCellEditor(parent, new String[] { Boolean.TRUE.toString(), Boolean.FALSE.toString()} , SWT.DROP_DOWN | SWT.READ_ONLY);
            } else {
                return new CustomCheckboxCellEditor(parent);
            }
            //return new CheckboxCellEditor(parent);
        } else if (propertyType.isEnum()) {
            final Object[] enumConstants = propertyType.getEnumConstants();
            final String[] strings = new String[enumConstants.length];
            for (int i = 0, itemsLength = enumConstants.length; i < itemsLength; i++) {
                strings[i] = ((Enum)enumConstants[i]).name();
            }
            return new CustomComboBoxCellEditor(
                parent,
                strings,
                SWT.DROP_DOWN | SWT.READ_ONLY);
        } else if (LocalDateTime.class.isAssignableFrom(propertyType)) {
            return new CustomLocalDateTimeCellEditor(parent);
        } else {
            log.warn("Unsupported property type: " + propertyType.getName());
            return null;
        }
    }

    private static void setValidator(CellEditor editor, DBPPropertyDescriptor property, Object object) {
        if (property instanceof ObjectPropertyDescriptor) {
            IPropertyValueValidator valueValidator = ((ObjectPropertyDescriptor) property).getValueValidator();
            if (valueValidator != null) {
                editor.setValidator(new PropertyCellEditorValidator(valueValidator, object));
            }
        }
    }

    private static class PropertyCellEditorValidator implements ICellEditorValidator {
        private final IPropertyValueValidator validator;
        private final Object object;

        PropertyCellEditorValidator(IPropertyValueValidator validator, Object object) {
            this.validator = validator;
            this.object = object;
        }

        @Override
        public String isValid(Object value) {
            try {
                boolean validValue = validator.isValidValue(object, value);
                if (validValue) {
                    return null;
                } else {
                    return "Invalid";
                }
            } catch (Exception e) {
                return e.getMessage();
            }
        }
    }
}
