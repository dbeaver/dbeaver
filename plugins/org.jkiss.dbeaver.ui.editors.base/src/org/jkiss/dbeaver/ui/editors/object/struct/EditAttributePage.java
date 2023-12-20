/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.editors.object.struct;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeConstrainable;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.internal.EditorsMessages;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * EditAttributePage
 */
public class EditAttributePage extends PropertyObjectEditPage<DBSTableColumn> {

    @NotNull
    private final Map<String, Object> options;
    private boolean isUnique;
    private List<Pair<DBSEntityConstraintType, Class<? extends DBSEntityConstraint>>> constraintTypes;

    private Pair<DBSEntityConstraintType, Class<? extends DBSEntityConstraint>> selectedConstraintType;
    private String selectedConstraintName;
    private Combo keyTypeCombo;
    private Text constraintNameText;

    public EditAttributePage(
        @Nullable DBECommandContext commandContext,
        @NotNull DBSTableColumn object,
        @NotNull Map<String, Object> options
    ) {
        super(commandContext, object);
        this.options = options;
    }

    @Override
    protected String getPropertiesGroupTitle() {
        return EditorsMessages.dialog_struct_label_text_properties;
    }

    @Override
    protected void createAdditionalEditControls(Composite composite) {
        if (getObject() instanceof DBSEntityAttributeConstrainable attributeConstrainable) {
            createKeysGroup(composite, attributeConstrainable);
        }
    }

    private void createKeysGroup(Composite composite, DBSEntityAttributeConstrainable attributeConstrainable) {
        constraintTypes = attributeConstrainable.getSupportedConstraints();
        if (CommonUtils.isEmpty(constraintTypes)) {
            return;
        }
        constraintTypes = constraintTypes.stream()
            .filter(ct -> AbstractTableConstraint.class.isAssignableFrom(ct.getSecond()))
            .collect(Collectors.toList());
        if (CommonUtils.isEmpty(constraintTypes)) {
            return;
        }

        Group keysGroup = UIUtils.createControlGroup(composite, "Keys", 3, GridData.FILL_HORIZONTAL, SWT.DEFAULT);

        Button uniqueCheck = UIUtils.createCheckbox(keysGroup, "Unique", "Mark column unique", false, 1);

        keyTypeCombo = UIUtils.createLabelCombo(keysGroup, "Type", "Constraint type", SWT.DROP_DOWN | SWT.READ_ONLY);
        keyTypeCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        for (Pair<DBSEntityConstraintType, Class<? extends DBSEntityConstraint>> ct : constraintTypes) {
            keyTypeCombo.add(ct.getFirst().getLocalizedName());
        }
        keyTypeCombo.setEnabled(false);

        uniqueCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> {
            isUnique = uniqueCheck.getSelection();
            keyTypeCombo.setEnabled(isUnique);
            constraintNameText.setEnabled(isUnique);
            if (isUnique && keyTypeCombo.getSelectionIndex() < 0) {
                keyTypeCombo.select(0);
            }
            updateConstraintType();
        }));
        keyTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateConstraintType();
            }
        });

        UIUtils.createEmptyLabel(keysGroup, 1, 1);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = 150;
        constraintNameText = UIUtils.createLabelText(keysGroup, "Name", null, SWT.BORDER, gd);
        constraintNameText.addModifyListener(e -> selectedConstraintName = constraintNameText.getText());
    }

    private void updateConstraintType() {
        int selectionIndex = keyTypeCombo.getSelectionIndex();
        if (selectionIndex < 0) {
            selectedConstraintType = null;
        } else {
            selectedConstraintType = constraintTypes.get(selectionIndex);
            selectedConstraintName = getObject().getParentObject().getName() + "_" + selectedConstraintType.getFirst().getId();
            constraintNameText.setText(selectedConstraintName);
        }
    }

    @Override
    public void performFinish() throws DBException {
        super.performFinish();

        if (isUnique && selectedConstraintType != null) {
            options.put(SQLObjectEditor.OPTION_ADDITIONAL_ACTION, (DBRRunnableWithProgress) monitor -> {
                try {
                    createConstrainForColumn(monitor);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        }
    }

    private void createConstrainForColumn(DBRProgressMonitor monitor) throws DBException {
        // Create constraint
        DBECommandContext commandContext = getCommandContext();

        DBEObjectManager<?> objectManager = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(selectedConstraintType.getSecond());
        if (objectManager == null) {
            throw new DBException("Object manager not found for type '" + selectedConstraintType.getSecond().getName() + "'");
        }
        DBEObjectMaker<?,?> objectMaker = (DBEObjectMaker<?,?>) objectManager;
        Map<String, Object> constrOptions = new LinkedHashMap<>(options);
        constrOptions.put(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true);
        DBSObject newConstraint = objectMaker.createNewObject(
            monitor,
            commandContext,
            getObject().getParentObject(),
            null,
            constrOptions);
        if (newConstraint instanceof AbstractTableConstraint<?,?> atc) {
            atc.setName(selectedConstraintName);
            atc.setConstraintType(selectedConstraintType.getFirst());
            atc.addAttributeReference(getObject());
        }
    }
}
