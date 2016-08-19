/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
