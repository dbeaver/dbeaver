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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerDatabank;
import org.jkiss.dbeaver.ext.mimer.model.MimerSequence;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

/**
 * "Create Sequence" dialog for Mimer SQL - all attributes are collected before
 * the sequence object is created.
 *
 * @author Mimer Information Technology
 */
public class MimerCreateSequencePage extends BaseObjectEditPage {

    private static final Log log = Log.getLog(MimerCreateSequencePage.class);
    private static final String[] DATA_TYPES = {"SMALLINT", "INTEGER", "BIGINT"};

    private final MimerSequence sequence;

    private String name = "";
    private String dataType = "INTEGER";
    private String startWith = "1";
    private String incrementBy = "1";
    private String minValue = "";
    private String maxValue = "";
    private boolean cycle;
    private String databank = "";

    public MimerCreateSequencePage(@NotNull MimerSequence sequence) {
        super("Create sequence");
        this.sequence = sequence;
    }

    @Override
    public DBSObject getObject() {
        return sequence;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabelText(group, "Schema",
            DBUtils.getObjectFullName(sequence.getParentObject(), DBPEvaluationContext.UI)).setEditable(false);

        Text nameText = UIUtils.createLabelText(group, "Name", "");
        nameText.addModifyListener(e -> {
            name = nameText.getText();
            updatePageState();
        });
        nameText.setFocus();

        Combo typeCombo = UIUtils.createLabelCombo(group, "Data type", SWT.DROP_DOWN | SWT.READ_ONLY);
        for (String t : DATA_TYPES) {
            typeCombo.add(t);
        }
        typeCombo.setText(dataType);
        typeCombo.addModifyListener(e -> dataType = typeCombo.getText());

        Text startText = UIUtils.createLabelText(group, "Start with", startWith);
        startText.addModifyListener(e -> startWith = startText.getText());

        Text incText = UIUtils.createLabelText(group, "Increment by", incrementBy);
        incText.addModifyListener(e -> incrementBy = incText.getText());

        Text minText = UIUtils.createLabelText(group, "Min value", "");
        minText.setMessage(MimerUIMessages.page_create_sequence_min_placeholder);
        minText.addModifyListener(e -> minValue = minText.getText());

        Text maxText = UIUtils.createLabelText(group, "Max value", "");
        maxText.setMessage(MimerUIMessages.page_create_sequence_max_placeholder);
        maxText.addModifyListener(e -> maxValue = maxText.getText());

        Button cycleCheck = UIUtils.createCheckbox(group, "Cycle", null, false, 2);
        cycleCheck.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                cycle = cycleCheck.getSelection();
            }
        });

        Combo dbCombo = UIUtils.createLabelCombo(group, "Databank", SWT.DROP_DOWN);
        dbCombo.add("");
        for (String db : loadDatabankNames()) {
            dbCombo.add(db);
        }
        dbCombo.addModifyListener(e -> databank = dbCombo.getText());

        return group;
    }

    @NotNull
    private String[] loadDatabankNames() {
        try {
            if (sequence.getDataSource() instanceof MimerDataSource ds) {
                return ds.getDatabanks(new VoidProgressMonitor()).stream()
                    .map(MimerDatabank::getName)
                    .filter(name -> !MimerConstants.SYSTEM_DATABANKS.contains(name))
                    .toArray(String[]::new);
            }
        } catch (Exception e) {
            log.debug("Can't load databank list for the create-sequence dialog", e);
        }
        return new String[0];
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(name);
    }

    /**
     * Applies the collected values to the sequence. Call after {@link #edit()} returns {@code true}.
     */
    public void applyChanges() {
        sequence.setName(name.trim());
        sequence.setDataType(dataType);
        sequence.setLastValue(parseLong(startWith, 1L));
        sequence.setIncrementBy(parseLong(incrementBy, 1L));
        sequence.setMinValue(parseLong(minValue, null));
        sequence.setMaxValue(parseLong(maxValue, null));
        sequence.setCycle(cycle);
        sequence.setDatabank(CommonUtils.isEmptyTrimmed(databank) ? null : databank.trim());
    }

    private static Long parseLong(String s, Long defaultValue) {
        if (CommonUtils.isEmptyTrimmed(s)) {
            return defaultValue;
        }
        try {
            return Long.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
