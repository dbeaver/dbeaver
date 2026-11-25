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
package org.jkiss.dbeaver.ui.forms;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.css.CSSUtils;

/** A description styled by the active Eclipse theme. */
public final class HintLabel extends Composite {
    public static final String CSS_CLASS = "dbeaverFormHint";
    public static final int PREFERRED_WIDTH_IN_LINE_HEIGHTS = 30;

    HintLabel(@NotNull Composite parent, @NotNull String text) {
        super(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().margins(0, 0).applyTo(this);

        Label label = UIControlFactory.createLabel(this, SWT.WRAP);
        CSSUtils.setCSSClass(label, CSS_CLASS);
        label.setText(text);
        var font = UIUtils.modifyFontSize(label.getFont(), -1);
        label.setFont(font);
        label.addDisposeListener(e -> font.dispose());
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = Math.min(
            label.computeSize(SWT.DEFAULT, SWT.DEFAULT).x,
            UIUtils.getFontHeight(label) * PREFERRED_WIDTH_IN_LINE_HEIGHTS
        );
        label.setLayoutData(data);
    }
}
