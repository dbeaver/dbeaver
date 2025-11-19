/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.widgets.SpinnerFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.UIUtils;

import java.time.Duration;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * A simple duration picker that allows to individually specify hours, minutes, and seconds.
 */
public final class DurationPickerDialog extends Dialog {
    private final String title;

    private int hours;
    private int minutes;
    private int seconds;

    public DurationPickerDialog(@NotNull Shell parentShell, @Nullable String title, @Nullable Duration duration) {
        super(parentShell);
        this.title = title;

        if (duration != null) {
            hours = Math.toIntExact(duration.toHours());
            minutes = duration.toMinutesPart();
            seconds = duration.toSecondsPart();
        }
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        if (title != null) {
            newShell.setText(title);
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        GridLayout layout = (GridLayout) composite.getLayout();
        layout.makeColumnsEqualWidth = true;
        layout.numColumns = 3;

        UIUtils.createControlLabel(composite, "Hours");
        UIUtils.createControlLabel(composite, "Minutes");
        UIUtils.createControlLabel(composite, "Seconds");

        createSpinner(composite, 23, () -> hours, value -> hours = value);
        createSpinner(composite, 59, () -> minutes, value -> minutes = value);
        createSpinner(composite, 59, () -> seconds, value -> seconds = value);

        UIUtils.asyncExec(this::updateCompletion);

        return composite;
    }

    private void createSpinner(@NotNull Composite parent, int maximum, @NotNull IntSupplier getter, @NotNull IntConsumer setter) {
        SpinnerFactory.newSpinner(SWT.BORDER)
            .bounds(0, maximum)
            .layoutData(GridDataFactory.fillDefaults().grab(true, false).create())
            .onSelect(e -> {
                setter.accept(((Spinner) e.widget).getSelection());
                updateCompletion();
            })
            .create(parent)
            .setSelection(getter.getAsInt());
    }

    @NotNull
    public Duration getDuration() {
        return Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
    }

    private void updateCompletion() {
        getButton(IDialogConstants.OK_ID).setEnabled(getDuration().isPositive());
    }
}
