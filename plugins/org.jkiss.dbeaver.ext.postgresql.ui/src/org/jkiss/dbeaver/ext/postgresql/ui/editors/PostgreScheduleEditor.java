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
package org.jkiss.dbeaver.ext.postgresql.ui.editors;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreJobSchedule;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionComment;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorUtils;

import java.time.DayOfWeek;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.IntStream;

public class PostgreScheduleEditor extends AbstractDatabaseObjectEditor<PostgreJobSchedule> {
    private final List<Runnable> listeners = new ArrayList<>();
    private final boolean[] minutes = new boolean[60];
    private final boolean[] hours = new boolean[24];
    private final boolean[] weekDays = new boolean[7];
    private final boolean[] monthDays = new boolean[32];
    private final boolean[] months = new boolean[12];

    private PageControl pageControl;
    private boolean loaded;

    @Override
    public void createPartControl(Composite parent) {
        this.pageControl = new PageControl(parent);

        final Composite composite = new Composite(pageControl, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));

        {
            final Composite category = new Composite(composite, SWT.NONE);
            category.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false));
            category.setLayout(new GridLayout(1, false));

            final Calendar calendar = Calendar.getInstance();

            listeners.add(createCheckboxPanel(
                UIUtils.createControlGroup(category, "Week Days", 4, GridData.FILL_HORIZONTAL, 0),
                DayOfWeek.values(),
                new Decorator<>() {
                    @NotNull
                    @Override
                    public String getText(@NotNull DayOfWeek value) {
                        return value.plus(calendar.getFirstDayOfWeek() - Calendar.MONDAY).getDisplayName(TextStyle.SHORT, Locale.getDefault());
                    }

                    @Override
                    public boolean getChecked(@NotNull DayOfWeek value) {
                        return weekDays[value.ordinal()];
                    }

                    @Override
                    public void setChecked(@NotNull DayOfWeek value, boolean checked) {
                        weekDays[value.ordinal()] = checked;
                        addScheduleChange();
                    }
                }
            ));

            listeners.add(createCheckboxPanel(
                UIUtils.createControlGroup(category, "Month Days", 4, GridData.FILL_HORIZONTAL, 0),
                IntStream.range(1, 33).boxed().toArray(Integer[]::new),
                new Decorator<>() {
                    @NotNull
                    @Override
                    public String getText(@NotNull Integer value) {
                        return value == 32 ? "<Last>" : nth(value.toString(), value);
                    }

                    @Override
                    public boolean getChecked(@NotNull Integer value) {
                        return monthDays[value - 1];
                    }

                    @Override
                    public void setChecked(@NotNull Integer value, boolean checked) {
                        monthDays[value - 1] = checked;
                        addScheduleChange();
                        refreshSchedulePresentation();
                    }

                    @Override
                    public boolean getEnabled(@NotNull Integer value) {
                        if (value > 31) {
                            return true;
                        }

                        for (int i = 0; i < months.length; i++) {
                            if (months[i] && Month.of(i + 1).minLength() < value) {
                                return false;
                            }
                        }

                        return true;
                    }
                }
            ));

            listeners.add(createCheckboxPanel(
                UIUtils.createControlGroup(category, "Months", 4, GridData.FILL_HORIZONTAL, 0),
                Month.values(),
                new Decorator<>() {
                    @NotNull
                    @Override
                    public String getText(@NotNull Month value) {
                        return value.getDisplayName(TextStyle.SHORT, Locale.getDefault());
                    }

                    @Override
                    public boolean getChecked(@NotNull Month value) {
                        return months[value.ordinal()];
                    }

                    @Override
                    public void setChecked(@NotNull Month value, boolean checked) {
                        months[value.ordinal()] = checked;
                        addScheduleChange();
                        refreshSchedulePresentation();
                    }

                    @Override
                    public boolean getEnabled(@NotNull Month month) {
                        for (int i = month.minLength(); i < 31; i++) {
                            if (monthDays[i]) {
                                return false;
                            }
                        }

                        return true;
                    }
                }
            ));
        }

        {
            final Composite category = new Composite(composite, SWT.NONE);
            category.setLayout(new GridLayout(1, false));
            category.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

            listeners.add(createCheckboxPanel(
                UIUtils.createControlGroup(category, "Hours", 6, GridData.FILL_HORIZONTAL, 0),
                IntStream.range(0, 24).boxed().toArray(Integer[]::new),
                new Decorator<>() {
                    @NotNull
                    @Override
                    public String getText(@NotNull Integer value) {
                        return value.toString();
                    }

                    @Override
                    public boolean getChecked(@NotNull Integer value) {
                        return hours[value];
                    }

                    @Override
                    public void setChecked(@NotNull Integer value, boolean checked) {
                        hours[value] = checked;
                        addScheduleChange();
                    }
                }
            ));

            listeners.add(createCheckboxPanel(
                UIUtils.createControlGroup(category, "Minutes", 6, GridData.FILL_HORIZONTAL, 0),
                IntStream.range(0, 60).boxed().toArray(Integer[]::new),
                new Decorator<>() {
                    @NotNull
                    @Override
                    public String getText(@NotNull Integer value) {
                        return value.toString();
                    }

                    @Override
                    public boolean getChecked(@NotNull Integer value) {
                        return minutes[value];
                    }

                    @Override
                    public void setChecked(@NotNull Integer value, boolean checked) {
                        minutes[value] = checked;
                        addScheduleChange();
                    }
                }
            ));
        }

        pageControl.createOrSubstituteProgressPanel(getSite());
    }

    @Override
    public void activatePart() {
        if (loaded) {
            return;
        }

        final PostgreJobSchedule schedule = getDatabaseObject();
        System.arraycopy(schedule.getMinutes(), 0, minutes, 0, minutes.length);
        System.arraycopy(schedule.getHours(), 0, hours, 0, hours.length);
        System.arraycopy(schedule.getWeekDays(), 0, weekDays, 0, weekDays.length);
        System.arraycopy(schedule.getMonthDays(), 0, monthDays, 0, monthDays.length);
        System.arraycopy(schedule.getMonths(), 0, months, 0, months.length);

        refreshSchedulePresentation();

        loaded = true;
    }

    @Override
    public RefreshResult refreshPart(Object source, boolean force) {
        if (force || !loaded || (source instanceof DBNEvent && ((DBNEvent) source).getSource() == DBNEvent.UPDATE_ON_SAVE)) {
            loaded = false;
            activatePart();
            return RefreshResult.REFRESHED;
        }

        return RefreshResult.IGNORED;
    }

    @Override
    public void setFocus() {
        if (pageControl != null) {
            pageControl.activate(true);
        }
    }

    private void refreshSchedulePresentation() {
        listeners.forEach(Runnable::run);
    }

    private void addScheduleChange() {
        addChangeCommand(new UpdateCommand(getDatabaseObject()), new DBECommandReflector<>() {
            @Override
            public void redoCommand(DBECommand<PostgreJobSchedule> command) {
                // not implemented
            }

            @Override
            public void undoCommand(DBECommand<PostgreJobSchedule> command) {
                // not implemented
            }
        });
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static <T> Runnable createCheckboxPanel(@NotNull Composite parent, @NotNull T[] input, @NotNull Decorator<T> decorator) {
        final int cols = ((GridLayout) parent.getLayout()).numColumns;
        final int rows = (int) Math.ceil(input.length / (float) cols);
        final Button[] buttons = new Button[input.length];
        final SelectionListener listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final Button button = (Button) e.widget;
                final T data = (T) button.getData();
                decorator.setChecked(data, button.getSelection());
            }
        };

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                final int index = i + j * rows;

                if (index < input.length) {
                    final T value = input[index];
                    final Button button = new Button(parent, SWT.CHECK);
                    button.setText(decorator.getText(value));
                    button.setData(value);
                    button.addSelectionListener(listener);
                    buttons[index] = button;
                } else {
                    UIUtils.createPlaceholder(parent, 1);
                }
            }
        }

        return () -> {
            for (Button button : buttons) {
                final T data = (T) button.getData();
                button.setText(decorator.getText(data));
                button.setSelection(decorator.getChecked(data));
                button.setEnabled(decorator.getEnabled(data));
            }
        };
    }

    @NotNull
    private static String nth(@NotNull String name, int number) {
        if (number <= 3 || number >= 21) {
            switch (number % 10) {
                case 1:
                    return name + "st";
                case 2:
                    return name + "nd";
                case 3:
                    return name + "rd";
                default:
                    break;
            }
        }
        return name + "th";
    }

    private interface Decorator<T> {
        @NotNull
        String getText(@NotNull T t);

        boolean getChecked(@NotNull T t);

        void setChecked(@NotNull T t, boolean checked);

        default boolean getEnabled(@NotNull T t) {
            return true;
        }
    }

    private class PageControl extends ProgressPageControl {
        public PageControl(@NotNull Composite parent) {
            super(parent, SWT.SHEET);
        }

        @Override
        public void fillCustomActions(@NotNull IContributionManager manager) {
            super.fillCustomActions(manager);

            final IWorkbenchSite site = getSite();
            if (site != null) {
                manager.add(new Separator());
                DatabaseEditorUtils.contributeStandardEditorActions(site, manager);
            }
        }
    }

    private class UpdateCommand extends DBECommandAbstract<PostgreJobSchedule> {
        public UpdateCommand(@NotNull PostgreJobSchedule object) {
            super(object, "Update schedule");
        }

        @Override
        public DBEPersistAction[] getPersistActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, Map<String, Object> options) {
            final PostgreJobSchedule schedule = getObject();
            final StringJoiner changes = new StringJoiner(",\n\t");

            if (!Arrays.equals(minutes, schedule.getMinutes())) {
                changes.add("jscminutes=" + toCompactArray(minutes));
            }

            if (!Arrays.equals(hours, schedule.getHours())) {
                changes.add("jschours=" + toCompactArray(hours));
            }

            if (!Arrays.equals(weekDays, schedule.getWeekDays())) {
                changes.add("jscweekdays=" + toCompactArray(weekDays));
            }

            if (!Arrays.equals(monthDays, schedule.getMonthDays())) {
                changes.add("jscmonthdays=" + toCompactArray(monthDays));
            }

            if (!Arrays.equals(months, schedule.getMonths())) {
                changes.add("jscmonths=" + toCompactArray(months));
            }

            if (changes.length() == 0) {
                return new DBEPersistAction[0];
            }

            return new DBEPersistAction[]{
                new SQLDatabasePersistActionComment(
                    schedule.getDataSource(),
                    "Update schedule '" + schedule.getName() + "'"
                ),
                new SQLDatabasePersistAction(
                    "Update schedule",
                    "UPDATE pgagent.pga_schedule\nSET\n\t" + changes + "\nWHERE jscid=" + schedule.getObjectId()
                )
            };
        }

        @Override
        public DBECommand<?> merge(DBECommand<?> prevCommand, Map<Object, Object> userParams) {
            final String name = "schedule#" + getObject().getObjectId();

            if (userParams.putIfAbsent(name, this) == null) {
                return this;
            }

            return (DBECommand<?>) userParams.get(name);
        }

        @NotNull
        private String toCompactArray(@NotNull boolean[] values) {
            final StringJoiner joiner = new StringJoiner(",", "'{", "}'");
            for (boolean value : values) {
                joiner.add(value ? "t" : "f");
            }
            return joiner.toString();
        }
    }
}
