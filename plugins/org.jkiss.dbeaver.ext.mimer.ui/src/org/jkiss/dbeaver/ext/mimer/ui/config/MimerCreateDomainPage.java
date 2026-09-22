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
import org.jkiss.dbeaver.ext.mimer.model.MimerDomain;
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
 * "Create Domain" dialog: Name, Data Type (free-typeable, since a domain can be based on any
 * built-in type or UDT) with a separate Size field, Collation, Default value, Constraint name,
 * and Check clause (all but Name/Data Type optional). All attributes are collected before the
 * object is created - see {@link MimerDomainConfigurator}.
 *
 * @author Mimer Information Technology
 */
public class MimerCreateDomainPage extends BaseObjectEditPage {

    private static final Log log = Log.getLog(MimerCreateDomainPage.class);

    private final MimerDomain domain;

    private String name = "";
    private String dataType = "";
    private String size = "";
    private String collation = "";
    private String defaultValue = "";
    private String constraintName = "";
    private String checkClause = "";

    public MimerCreateDomainPage(@NotNull MimerDomain domain) {
        super("Create domain");
        this.domain = domain;
    }

    @Override
    public DBSObject getObject() {
        return domain;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabelText(group, "Schema",
            DBUtils.getObjectFullName(domain.getParentObject(), DBPEvaluationContext.UI)).setEditable(false);

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
        sizeText.setMessage(MimerUIMessages.page_create_domain_size_placeholder);
        sizeText.addModifyListener(e -> size = sizeText.getText());

        Combo collationCombo = UIUtils.createLabelCombo(group, "Collation", SWT.DROP_DOWN);
        collationCombo.add("");
        for (String c : loadCollationNames()) {
            collationCombo.add(c);
        }
        collationCombo.addModifyListener(e -> collation = collationCombo.getText());

        Text defaultText = UIUtils.createLabelText(group, "Default value", "");
        defaultText.setMessage(MimerUIMessages.page_create_domain_default_placeholder);
        defaultText.addModifyListener(e -> defaultValue = defaultText.getText());

        Text constraintText = UIUtils.createLabelText(group, "Constraint name", "");
        constraintText.setMessage(MimerUIMessages.page_create_domain_constraint_name_placeholder);
        constraintText.addModifyListener(e -> constraintName = constraintText.getText());

        Text checkText = UIUtils.createLabelText(group, "Check", "");
        checkText.setMessage(MimerUIMessages.page_create_domain_check_placeholder);
        checkText.addModifyListener(e -> checkClause = checkText.getText());

        return group;
    }

    /**
     * Strips the driver's literal, useless {@code (...)} placeholder suffix on sized type names
     * (e.g. {@code "CHARACTER VARYING()"}) so the combo shows a clean base name - size is
     * collected separately via {@link #size}.
     */
    @NotNull
    private String[] loadDataTypeNames() {
        try {
            Set<String> names = new LinkedHashSet<>();
            for (DBSObject type : domain.getDataSource().getDataTypes(new VoidProgressMonitor())) {
                names.add(type.getName().replaceAll("\\(.*\\)", "").trim());
            }
            return names.toArray(new String[0]);
        } catch (Exception e) {
            log.debug("Can't load data type list for the create-domain dialog", e);
        }
        return new String[0];
    }

    @NotNull
    private String[] loadCollationNames() {
        try {
            if (domain.getDataSource() instanceof MimerDataSource ds) {
                return ds.getCollations(new VoidProgressMonitor()).stream()
                    .map(c -> "\"" + c.getSchemaName() + "\".\"" + c.getName() + "\"")
                    .toArray(String[]::new);
            }
        } catch (Exception e) {
            log.debug("Can't load collation list for the create-domain dialog", e);
        }
        return new String[0];
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(name) && !CommonUtils.isEmptyTrimmed(dataType);
    }

    /**
     * Applies the collected values to the domain. Call after {@link #edit()} returns {@code true}.
     */
    public void applyChanges() {
        domain.setName(name.trim());
        domain.setDataType(CommonUtils.isEmptyTrimmed(size) ? dataType.trim() : dataType.trim() + "(" + size.trim() + ")");
        domain.setCollation(CommonUtils.isEmptyTrimmed(collation) ? null : collation.trim());
        domain.setDefaultValue(CommonUtils.isEmptyTrimmed(defaultValue) ? null : defaultValue.trim());
        domain.setConstraintName(CommonUtils.isEmptyTrimmed(constraintName) ? null : constraintName.trim());
        domain.setCheckClause(CommonUtils.isEmptyTrimmed(checkClause) ? null : checkClause.trim());
    }
}
