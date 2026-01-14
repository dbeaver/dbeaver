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

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.jkiss.dbeaver.ui.editors.erd.figures;

import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Toggle;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;

public final class CustomCheckBoxFigure extends Toggle {
    private Label label;
    private static final Image UNCHECKED = DBeaverIcons.getImage(UIIcon.CHECK_OFF);
    private static final Image CHECKED = DBeaverIcons.getImage(UIIcon.CHECK_ON);

    public CustomCheckBoxFigure() {
        this("");
        setFocusTraversable(false);
        setRequestFocusEnabled(false);
    }

    public CustomCheckBoxFigure(String text) {
        this.label = new Label(text, UNCHECKED);
        this.setContents(this.label);
    }

    protected void handleSelectionChanged() {
        if (this.isSelected()) {
            this.label.setIcon(CHECKED);
        } else {
            this.label.setIcon(UNCHECKED);
        }

    }

    protected void init() {
        super.init();
        this.addChangeListener(changeEvent -> {
            if (changeEvent.getPropertyName().equals("selected")) {
                CustomCheckBoxFigure.this.handleSelectionChanged();
            }

        });
    }
}
