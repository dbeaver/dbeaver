/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * ValueFormatSelector
 */
public class ValueFormatSelector {

    private final Combo formatCombo;

    public ValueFormatSelector(@NotNull Composite parent) {
        UIUtils.createControlLabel(parent, "Value Format");
        formatCombo = new Combo(parent, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
        formatCombo.add("Display (default)");
        formatCombo.add("Editable");
        formatCombo.add("Database native");
    }

    public void select(@NotNull DBDDisplayFormat format) {
        formatCombo.select(format == DBDDisplayFormat.UI ? 0 : format == DBDDisplayFormat.EDIT ? 1 : 2);
    }

    @NotNull
    public DBDDisplayFormat getSelection() {
        switch (formatCombo.getSelectionIndex()) {
            case 0: return DBDDisplayFormat.UI;
            case 1: return DBDDisplayFormat.EDIT;
            default: return DBDDisplayFormat.NATIVE;
        }
    }

}
