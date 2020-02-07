package org.jkiss.dbeaver.ui.contentassist;

import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.UIUtils;

public class SmartTextContentAdapter extends TextContentAdapter {

    private static final Log log = Log.getLog(UIUtils.class);

    public SmartTextContentAdapter() {
    }

    @Override
    public void insertControlContents(Control control, String contents, int cursorPosition) {
        Text text = (Text) control;
        String curValue = text.getText();
        Point selection = text.getSelection();

        if (selection.x == selection.y) {
            // Try to replace text under cursor contents starts with
            for (int i = selection.x - 1; i >= 0; i--) {
                String prefix = curValue.substring(i, selection.x);
                if (i > 0 && contents.startsWith(prefix)) {
                    text.setSelection(i, selection.x);
                    break;
                }
                char ch = curValue.charAt(i);
                if (!Character.isLetterOrDigit(ch) && contents.indexOf(ch) == -1) {
                    // Work break
                    break;
                }
            }
        }
        text.insert(contents);

        // Insert will leave the cursor at the end of the inserted text. If this
        // is not what we wanted, reset the selection.
        if (cursorPosition < contents.length()) {
            text.setSelection(selection.x + cursorPosition,
                selection.x + cursorPosition);
        }
    }

}
