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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBeaverIcons;

/** An icon aligned with the first line of a wrapping information message. */
public final class CInfoLabel extends Composite {
    private final Label iconLabel;
    private final Label textLabel;

    public CInfoLabel(@NotNull Composite parent, @NotNull String text, @NotNull DBPImage image, int widthHint) {
        super(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(this);
        setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        iconLabel = new Label(this, SWT.NONE);
        iconLabel.setImage(DBeaverIcons.getImage(image));
        iconLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

        textLabel = new Label(this, SWT.WRAP);
        textLabel.setText(text);
        GridData textData = new GridData(GridData.FILL_HORIZONTAL);
        textData.widthHint = widthHint;
        textLabel.setLayoutData(textData);
    }

    @NotNull
    public Label getTextLabel() {
        return textLabel;
    }

    public void setText(@NotNull String text) {
        textLabel.setText(text);
        textLabel.requestLayout();
    }

    public void setImage(@NotNull DBPImage image) {
        iconLabel.setImage(DBeaverIcons.getImage(image));
        iconLabel.requestLayout();
    }
}
