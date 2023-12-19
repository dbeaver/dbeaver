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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.internal.EditorsMessages;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EditAttributePage
 */
public class EditAttributePage extends PropertyObjectEditPage<DBSEntityAttribute> {

    @NotNull
    private final Map<String, Object> options;
    private boolean isUnique;
    private List<Pair<DBSEntityConstraintType, Class<? extends DBSEntityConstraint>>> constraintTypes;

    private Pair<DBSEntityConstraintType, Class<? extends DBSEntityConstraint>> selectedConstraintType;
    private String selectedConstraintName;

    public EditAttributePage(
        @Nullable DBECommandContext commandContext,
        @NotNull DBSEntityAttribute object,
        @NotNull Map<String, Object> options
    ) {
        super(commandContext, object);
        this.options = options;
    }

    @Override
    protected String getPropertiesGroupTitle() {
        return EditorsMessages.dialog_struct_label_text_properties;
    }

    protected DBSEntityConstraintType[] getPossibleConstraintTypes() {
        return new DBSEntityConstraintType[] {
            DBSEntityConstraintType.PRIMARY_KEY,
            DBSEntityConstraintType.UNIQUE_KEY,
            DBSEntityConstraintType.INDEX,
        };
    }

    @Override
    protected void createAdditionalEditControls(Composite composite) {
        if (getObject() instanceof DBSEntityAttributeConstrainable attributeConstrainable) {
            createKeysGroup(composite, attributeConstrainable);
        }
    }

    private void createKeysGroup(Composite composite, DBSEntityAttributeConstrainable attributeConstrainable) {
        Group keysGroup = UIUtils.createControlGroup(composite, "Keys", 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);

        DBSEntity entity = getObject().getParentObject();
        //entity.getConstraints()
        Button uniqueCheck = UIUtils.createCheckbox(keysGroup, "Unique", "Mark column unique", false, 1);

        Combo keyTypeCombo = new Combo(keysGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        keyTypeCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        constraintTypes = attributeConstrainable.getSupportedConstraints();
        for (Pair<DBSEntityConstraintType, Class<? extends DBSEntityConstraint>> ct : constraintTypes) {
            keyTypeCombo.add(ct.getFirst().getLocalizedName());
        }
        keyTypeCombo.setEnabled(false);

        uniqueCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> {
            keyTypeCombo.setEnabled(uniqueCheck.getSelection());
            isUnique = uniqueCheck.getSelection();
            if (isUnique && keyTypeCombo.getSelectionIndex() < 0) {
                keyTypeCombo.select(0);
                selectedConstraintType = constraintTypes.get(0);
            }
        }));
        keyTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int selectionIndex = keyTypeCombo.getSelectionIndex();
                if (selectionIndex < 0) {
                    selectedConstraintType = null;
                } else {
                    selectedConstraintType = constraintTypes.get(selectionIndex);
                }
            }
        });
    }

    @Override
    public void performFinish() throws DBException {
        super.performFinish();

        if (isUnique && selectedConstraintType != null) {
            // Create constraint
            DBECommandContext commandContext = getCommandContext();

            DBEObjectManager<?> objectManager = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(selectedConstraintType.getSecond());
            if (objectManager == null) {
                throw new DBException("Object manager not found for type '" + selectedConstraintType.getSecond().getName() + "'");
            }
            DBEObjectMaker objectMaker = (DBEObjectMaker) objectManager;
            Map<String, Object> constrOptions = new LinkedHashMap<>(options);
            constrOptions.put(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true);
            DBSObject newConstraint = objectMaker.createNewObject(
                new VoidProgressMonitor(),
                commandContext,
                getObject().getParentObject(),
                null,
                constrOptions);
            if (!CommonUtils.isEmpty(selectedConstraintName) && newConstraint instanceof DBPNamedObject2 no2) {
                no2.setName(selectedConstraintName);
            }
            if (newConstraint instanceof AbstractTableConstraint atc) {
                atc.setConstraintType(selectedConstraintType.getFirst());
            }
        }
    }
}
