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
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.ext.mimer.model.MimerTableTrigger;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

/**
 * "Create Trigger" dialog for a Mimer SQL table or view trigger - name, Timing (BEFORE/AFTER for
 * a table; fixed {@code INSTEAD OF} for a view, since Mimer SQL only allows {@code INSTEAD OF} on a
 * view), Event (INSERT/UPDATE/DELETE, single choice) and, for a table trigger only, Granularity
 * (FOR EACH ROW/STATEMENT - a view trigger is always statement-level). Seeds a full {@code
 * CREATE TRIGGER ... BEGIN ATOMIC ... END} template into the object's source, finished off via
 * the Source tab that opens after this dialog closes. The {@code REFERENCING} clause is built
 * from the chosen event ({@code NEW} for INSERT, {@code OLD} for DELETE, both for UPDATE), using
 * {@code OLD ROW}/{@code NEW ROW} only for a row trigger, {@code OLD TABLE}/{@code NEW TABLE}
 * otherwise (always the latter for a view).
 *
 * @author Mimer Information Technology
 */
public class MimerCreateTriggerPage extends BaseObjectEditPage {

    private static final String INSTEAD_OF = "INSTEAD OF";
    private static final String[] TIMINGS = {"BEFORE", "AFTER"};
    private static final String[] EVENTS = {"INSERT", "UPDATE", "DELETE"};
    private static final String[] GRANULARITIES = {"FOR EACH STATEMENT", "FOR EACH ROW"};

    private final MimerTableTrigger trigger;
    private final boolean isView;

    private String name = "";
    private String timing;
    private String event = EVENTS[0];
    private String granularity = GRANULARITIES[0];

    public MimerCreateTriggerPage(@NotNull MimerTableTrigger trigger) {
        super("Create trigger");
        this.trigger = trigger;
        this.isView = trigger.getTable() instanceof GenericView;
        this.timing = isView ? INSTEAD_OF : TIMINGS[1];
    }

    @Override
    public DBSObject getObject() {
        return trigger;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabelText(group, isView ? "View" : "Table",
            DBUtils.getObjectFullName(trigger.getParentObject(), DBPEvaluationContext.UI)).setEditable(false);

        Text nameText = UIUtils.createLabelText(group, "Name", "");
        nameText.addModifyListener(e -> {
            name = nameText.getText();
            updatePageState();
        });
        nameText.setFocus();

        if (isView) {
            // Mimer SQL only allows INSTEAD OF on a view, never a physical table - nothing to choose.
            UIUtils.createLabelText(group, "Timing", INSTEAD_OF).setEditable(false);
        } else {
            Combo timingCombo = UIUtils.createLabelCombo(group, "Timing", SWT.READ_ONLY | SWT.DROP_DOWN);
            for (String t : TIMINGS) {
                timingCombo.add(t);
            }
            timingCombo.select(1);
            timingCombo.addModifyListener(e -> timing = timingCombo.getText());
        }

        Combo eventCombo = UIUtils.createLabelCombo(group, "Event", SWT.READ_ONLY | SWT.DROP_DOWN);
        for (String ev : EVENTS) {
            eventCombo.add(ev);
        }
        eventCombo.select(0);
        eventCombo.addModifyListener(e -> event = eventCombo.getText());

        if (!isView) {
            // A view/INSTEAD OF trigger is always statement-level - Mimer SQL offers no row-level
            // option for a view trigger.
            Combo granularityCombo = UIUtils.createLabelCombo(group, "Granularity", SWT.READ_ONLY | SWT.DROP_DOWN);
            for (String g : GRANULARITIES) {
                granularityCombo.add(g);
            }
            granularityCombo.select(0);
            granularityCombo.addModifyListener(e -> granularity = granularityCombo.getText());
        }

        return group;
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(name);
    }

    /**
     * Applies the collected clauses and seeds the initial template. Call after {@link #edit()}
     * returns {@code true}.
     */
    public void applyChanges() {
        String triggerName = name.trim();
        trigger.setName(triggerName);

        boolean forEachRow = granularity.equals(GRANULARITIES[1]);
        boolean needsOld = !event.equals(EVENTS[0]); // not INSERT
        boolean needsNew = !event.equals(EVENTS[2]); // not DELETE

        String schemaName = trigger.getTable().getSchema().getName();
        String tableName = trigger.getTable().getName();
        String kind = forEachRow ? "ROW" : "TABLE";

        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TRIGGER \"").append(schemaName).append("\".\"").append(triggerName).append("\"\n");
        sb.append(timing).append(' ').append(event).append('\n');
        sb.append("ON \"").append(schemaName).append("\".\"").append(tableName).append("\"\n");
        if (needsNew || needsOld) {
            sb.append("REFERENCING");
            if (needsNew) {
                sb.append(" NEW ").append(kind).append(" AS NEW_").append(kind);
            }
            if (needsOld) {
                sb.append(" OLD ").append(kind).append(" AS OLD_").append(kind);
            }
            sb.append('\n');
        }
        if (forEachRow) {
            sb.append("FOR EACH ROW\n");
        }
        sb.append("BEGIN ATOMIC\n");
        sb.append("    -- TODO: trigger body\n");
        sb.append("END");

        trigger.setObjectDefinitionText(sb.toString());
    }
}
