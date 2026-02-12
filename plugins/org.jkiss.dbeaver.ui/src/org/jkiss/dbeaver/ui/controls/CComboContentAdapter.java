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

import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.fieldassist.IControlContentAdapter2;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.ui.UIUtils;

public class CComboContentAdapter implements IControlContentAdapter, IControlContentAdapter2 {

    @Override
    public String getControlContents(Control control) {
        return ((CCombo) control).getText();
    }

    @Override
    public void setControlContents(Control control, String text,
                                   int cursorPosition) {
        ((CCombo) control).setText(text);
        ((CCombo) control)
            .setSelection(new Point(cursorPosition, cursorPosition));
    }

    @Override
    public void insertControlContents(Control control, String text,
                                      int cursorPosition) {
        CCombo combo = (CCombo) control;
        combo.setText(text);
        combo.setSelection(new Point(0, text.length()));
    }

    @Override
    public int getCursorPosition(Control control) {
        return ((CCombo) control).getSelection().x;
    }

    @Override
    public Rectangle getInsertionBounds(Control control) {
        // This doesn't take horizontal scrolling into affect.
        // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=204599
        CCombo combo = (CCombo) control;
        int position = combo.getSelection().y;
        String contents = combo.getText();
        Point extent = UIUtils.getTextSize(combo,
            contents.substring(0, Math.min(position,
                contents.length())));
        return new Rectangle(combo.getClientArea().x + extent.x, combo
            .getClientArea().y, 1, combo.getClientArea().height);
    }

    @Override
    public void setCursorPosition(Control control, int index) {
        ((CCombo) control).setSelection(new Point(index, index));
    }

    @Override
    public Point getSelection(Control control) {
        return ((CCombo) control).getSelection();
    }

    @Override
    public void setSelection(Control control, Point range) {
        ((CCombo) control).setSelection(range);
    }

}
