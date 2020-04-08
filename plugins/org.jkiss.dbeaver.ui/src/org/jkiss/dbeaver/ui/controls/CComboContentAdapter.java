package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.fieldassist.IControlContentAdapter2;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.ui.UIUtils;

public class CComboContentAdapter implements IControlContentAdapter, IControlContentAdapter2 {

    /*
     * Set to <code>true</code> if we should compute the text
     * vertical bounds rather than just use the field size.
     * Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=164748
     * The corresponding SWT bug is
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=44072
     */
    private static final boolean COMPUTE_TEXT_USING_CLIENTAREA = !Util.isCarbon();


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
        if (COMPUTE_TEXT_USING_CLIENTAREA) {
            return new Rectangle(combo.getClientArea().x + extent.x, combo
                .getClientArea().y, 1, combo.getClientArea().height);
        }
        return new Rectangle(extent.x, 0, 1, combo.getSize().y);
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
