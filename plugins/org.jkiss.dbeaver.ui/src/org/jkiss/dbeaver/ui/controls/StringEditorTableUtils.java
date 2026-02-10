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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Table with editable string rows
 */
public class StringEditorTableUtils {

    @NotNull
    public static Table createEditableList(
        @NotNull Composite parent,
        @NotNull String name,
        @Nullable List<String> values,
        @Nullable DBPImage icon,
        @Nullable IContentProposalProvider proposalProvider
    ) {
        return createCustomEditableList(
            parent,
            name,
            values,
            new StringEditorTableFactory.StringValuesManager(icon),
            proposalProvider,
            false
        );
    }

    @NotNull
    public static <T> Table createCustomEditableList(
        @NotNull Composite parent,
        @NotNull String name,
        @Nullable List<T> values,
        @NotNull TableValuesManager<T> valuesManager,
        @Nullable IContentProposalProvider proposalProvider,
        boolean withReordering
    ) {
        Composite group = UIUtils.createTitledComposite(parent, name, 2, GridData.FILL_BOTH);

        var stringEditorTableFactory = new StringEditorTableFactory<>(
            group,
            values,
            valuesManager,
            proposalProvider,
            withReordering
        );

        return stringEditorTableFactory.createTable();
    }

    public static void replaceAllStringValues(@NotNull Table valueTable, @Nullable List<String> values, @Nullable DBPImage icon) {
        valueTable.removeAll();
        if (!CommonUtils.isEmpty(values)) {
            for (String value : values) {
                TableItem tableItem = new TableItem(valueTable, SWT.LEFT);
                tableItem.setText(value);
                setCustomValue(tableItem, value);
                if (icon != null) {
                    tableItem.setImage(DBeaverIcons.getImage(icon));
                }
            }
        }
    }

    @NotNull
    public static List<String> collectStringValues(@NotNull Table table) {
        List<String> values = new ArrayList<>();
        for (TableItem item : table.getItems()) {
            String value = item.getText().trim();
            if (value.isEmpty()) {
                continue;
            }
            values.add(value);
        }
        return values;
    }

    @NotNull
    public static <T> List<T> collectCustomValues(@NotNull Table table) {
        List<T> values = new ArrayList<>(table.getItemCount());
        for (TableItem item : table.getItems()) {
            T value = getCustomValue(item);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    @NotNull
    private static <T> T getCustomValue(@NotNull TableItem tableItem) {
        return (T) tableItem.getData(StringEditorTableFactory.CUSTOM_EDITABLE_LIST_VALUE_KEY);
    }

    private static <T> void setCustomValue(@NotNull TableItem tableItem, @NotNull T value) {
        tableItem.setData(StringEditorTableFactory.CUSTOM_EDITABLE_LIST_VALUE_KEY, value);
    }

    /**
     * Manager of the custom values handled by StringEditorTable
     */
    public interface TableValuesManager<T> {

        @Nullable
        DBPImage getIcon(@Nullable T value);

        @NotNull
        String getString(@Nullable T value);

        @NotNull
        Boolean isEditable(@Nullable T value);

        @Nullable
        T prepareNewValue(@Nullable T originalValue, @Nullable String string);
    }
}
