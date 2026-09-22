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
package org.jkiss.dbeaver.ext.mimer.ui.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerUserDefinedType;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * "Create Distinct Type" dialog: Name, Data Type (free-typeable, since a distinct type can be
 * based on any built-in type or UDT) with a separate Size field, and Collation - same shape as
 * {@link MimerCreateDomainPage}, minus Default/Constraint/Check (a distinct type has no default
 * value or check clause, unlike a domain). All attributes are collected before the object is
 * created - see {@link MimerUserDefinedTypeConfigurator}.
 *
 * @author Mimer Information Technology
 */
public class MimerCreateDistinctTypePage extends BaseObjectEditPage {

    private static final Log log = Log.getLog(MimerCreateDistinctTypePage.class);

    private final MimerUserDefinedType type;

    private String name = "";
    private String dataType = "";
    private String size = "";
    private String collation = "";

    public MimerCreateDistinctTypePage(@NotNull MimerUserDefinedType type) {
        super("Create distinct type");
        this.type = type;
    }

    @Override
    public DBSObject getObject() {
        return type;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabelText(group, "Schema",
            DBUtils.getObjectFullName(type.getParentObject(), DBPEvaluationContext.UI)).setEditable(false);

        Text nameText = UIUtils.createLabelText(group, "Name", "");
        nameText.addModifyListener(e -> {
            name = nameText.getText();
            updatePageState();
        });
        nameText.setFocus();

        Combo typeCombo = UIUtils.createLabelCombo(group, "Data type", SWT.DROP_DOWN);
        typeCombo.setToolTipText(MimerUIMessages.tooltip_data_type);
        for (String t : loadDataTypeNames()) {
            typeCombo.add(t);
        }
        typeCombo.addModifyListener(e -> {
            dataType = typeCombo.getText();
            updatePageState();
        });

        Text sizeText = UIUtils.createLabelText(group, "Size", "");
        sizeText.setMessage(MimerUIMessages.page_create_distinct_type_size_placeholder);
        sizeText.addModifyListener(e -> size = sizeText.getText());

        Combo collationCombo = UIUtils.createLabelCombo(group, "Collation", SWT.DROP_DOWN);
        collationCombo.add("");
        for (String c : loadCollationNames()) {
            collationCombo.add(c);
        }
        collationCombo.addModifyListener(e -> collation = collationCombo.getText());

        return group;
    }

    @NotNull
    private String[] loadDataTypeNames() {
        try {
            Set<String> names = new LinkedHashSet<>();
            for (DBSObject t : type.getDataSource().getDataTypes(new VoidProgressMonitor())) {
                names.add(t.getName().replaceAll("\\(.*\\)", "").trim());
            }
            return names.toArray(new String[0]);
        } catch (Exception e) {
            log.debug("Can't load data type list for the create-distinct-type dialog", e);
        }
        return new String[0];
    }

    @NotNull
    private String[] loadCollationNames() {
        try {
            if (type.getDataSource() instanceof MimerDataSource ds) {
                return ds.getCollations(new VoidProgressMonitor()).stream()
                    .map(c -> "\"" + c.getSchemaName() + "\".\"" + c.getName() + "\"")
                    .toArray(String[]::new);
            }
        } catch (Exception e) {
            log.debug("Can't load collation list for the create-distinct-type dialog", e);
        }
        return new String[0];
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(name) && !CommonUtils.isEmptyTrimmed(dataType);
    }

    /**
     * Applies the collected values to the type. Call after {@link #edit()} returns {@code true}.
     */
    public void applyChanges() {
        type.setName(name.trim());
        type.setDataType(CommonUtils.isEmptyTrimmed(size) ? dataType.trim() : dataType.trim() + "(" + size.trim() + ")");
        type.setCollation(CommonUtils.isEmptyTrimmed(collation) ? null : collation.trim());
    }
}
