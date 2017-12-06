/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.data.managers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.editors.BaseValueEditor;
import org.jkiss.dbeaver.ui.dialogs.data.DefaultValueViewDialog;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * Abstract Enum/Set value manager
 */
public abstract class EnumValueManager extends BaseValueManager {

    @NotNull
    @Override
    public IValueController.EditType[] getSupportedEditTypes() {
        return new IValueController.EditType[] {IValueController.EditType.INLINE, IValueController.EditType.PANEL, IValueController.EditType.EDITOR};
    }

    @Override
    public IValueEditor createEditor(@NotNull final IValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case INLINE:
            {
                if (isMultiValue(controller)) {
                    return null;
                }
                return new EnumInlineEditor(controller);
            }
            case PANEL:
            {
                return new EnumPanelEditor(controller);
            }
            case EDITOR:
                return new DefaultValueViewDialog(controller);
            default:
                return null;
        }
    }

    protected abstract boolean isMultiValue(IValueController valueController);

    protected abstract List<String> getEnumValues(IValueController valueController);

    protected abstract List<String> getSetValues(IValueController valueController, Object value);

    private class EnumInlineEditor extends BaseValueEditor<Combo> {
        private final IValueController controller;

        public EnumInlineEditor(IValueController controller) {
            super(controller);
            this.controller = controller;
            setAutoSaveEnabled(true);
        }

        @Override
        public void primeEditorValue(@Nullable Object value) throws DBException
        {
            control.setText(DBUtils.isNullValue(value) ? "" : DBValueFormatting.getDefaultValueDisplayString(value, DBDDisplayFormat.UI));
        }

        @Override
        public Object extractEditorValue()
        {
            int selIndex = control.getSelectionIndex();
            if (selIndex < 0) {
                return null;
            } else {
                return control.getItem(selIndex);
            }
        }

        @Override
        protected Combo createControl(Composite editPlaceholder)
        {
            final Combo editor = new Combo(controller.getEditPlaceholder(), SWT.READ_ONLY);
            Collection<String> enumValues = getEnumValues(valueController);
            if (enumValues != null) {
                for (String enumValue : enumValues) {
                    editor.add(enumValue);
                }
            }
            if (editor.getSelectionIndex() < 0) {
                editor.select(0);
            }
            editor.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    saveValue();
                }
            });
            return editor;
        }
    }

    private class EnumPanelEditor extends BaseValueEditor<org.eclipse.swt.widgets.List> {
        private final IValueController controller;

        public EnumPanelEditor(IValueController controller) {
            super(controller);
            this.controller = controller;
        }

        @Override
        public void primeEditorValue(@Nullable Object value) throws DBException
        {
            if (isMultiValue(valueController)) {
                fillSetList(valueController, control, value);
            } else {
                if (value == null) {
                    control.setSelection(-1);
                }
                int itemCount = control.getItemCount();
                if (itemCount == 0) {
                    if (!DBUtils.isNullValue(value)) {
                        control.add(CommonUtils.toString(value));
                    }
                } else {
                    for (int i = 0; i < itemCount; i++) {
                        if (control.getItem(i).equals(value)) {
                            control.setSelection(i);
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public Object extractEditorValue()
        {
            if (isMultiValue(valueController)) {
                StringBuilder setString = new StringBuilder();
                for (String sel : control.getSelection()) {
                    if (setString.length() > 0) setString.append(',');
                    setString.append(sel);
                }
                return setString.toString();
            } else {
                int selIndex = control.getSelectionIndex();
                if (selIndex < 0) {
                    return null;
                } else {
                    return control.getItem(selIndex);
                }
            }
        }

        @Override
        protected org.eclipse.swt.widgets.List createControl(Composite editPlaceholder)
        {
            int style = SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL;
            if (isMultiValue(valueController)) {
                style |= SWT.MULTI;
            } else {
                style |= SWT.SINGLE;
            }
            final org.eclipse.swt.widgets.List editor = new org.eclipse.swt.widgets.List(editPlaceholder, style);
            Collection<String> enumValues = getEnumValues(valueController);
            if (enumValues != null) {
                for (String ev : enumValues) {
                    editor.add(ev);
                }
            }
            if (editor.getSelectionIndex() < 0) {
                editor.select(0);
            }
            if (controller.getEditType() == IValueController.EditType.INLINE) {
                editor.setFocus();
            }
            return editor;
        }
    }

    void fillSetList(IValueController valueController, org.eclipse.swt.widgets.List editor, Object value)
    {
        editor.removeAll();
        List<String> enumValues = getEnumValues(valueController);
        List<String> setValues = getSetValues(valueController, value);
        if (enumValues != null) {
            int[] selIndices = new int[setValues.size()];
            int selIndex = 0;
            for (int i = 0; i < enumValues.size(); i++) {
                String enumValue = enumValues.get(i);
                editor.add(enumValue);
                if (setValues.contains(enumValue)) {
                    selIndices[selIndex++] = i;
                }
            }
            editor.select(selIndices);
        } else {
            if (editor.getItemCount() == 0) {
                editor.add(CommonUtils.toString(value));
            }
        }
    }


}