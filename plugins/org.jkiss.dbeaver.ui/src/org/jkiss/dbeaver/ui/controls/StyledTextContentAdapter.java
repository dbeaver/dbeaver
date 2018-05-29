/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

/**
 * StyledTextContentAdapter
 */
public class StyledTextContentAdapter implements IControlContentAdapter, IControlContentAdapter2 {

    private final StyledText filtersText;

    public StyledTextContentAdapter(StyledText filtersText) {
        this.filtersText = filtersText;
    }

    @Override
    public String getControlContents(Control control) {
        return filtersText.getText();
    }

    @Override
    public void setControlContents(Control control, String text, int cursorPosition) {
        filtersText.setText(text);
        filtersText.setSelection(cursorPosition, cursorPosition);
    }

    @Override
    public void insertControlContents(Control control, String text, int cursorPosition) {
        Point selection = filtersText.getSelection();
        filtersText.insert(text);
        // Insert will leave the cursor at the end of the inserted text. If this
        // is not what we wanted, reset the selection.
        if (cursorPosition <= text.length()) {
            filtersText.setSelection(selection.x + cursorPosition, selection.x + cursorPosition);
        }
    }

    @Override
    public int getCursorPosition(Control control) {
        return filtersText.getCaretOffset();
    }

    @Override
    public Rectangle getInsertionBounds(Control control) {
        Point caretOrigin = filtersText.getLocationAtOffset(filtersText.getCaretOffset());
        // We fudge the y pixels due to problems with getCaretLocation
        // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=52520
        return new Rectangle(
            caretOrigin.x + filtersText.getClientArea().x,
            caretOrigin.y + filtersText.getClientArea().y + 3, 1, filtersText.getLineHeight());
    }

    @Override
    public void setCursorPosition(Control control, int position) {
        filtersText.setSelection(new Point(position, position));
    }

    @Override
    public Point getSelection(Control control) {
        return filtersText.getSelection();
    }

    @Override
    public void setSelection(Control control, Point range) {
        filtersText.setSelection(range);
    }
}
