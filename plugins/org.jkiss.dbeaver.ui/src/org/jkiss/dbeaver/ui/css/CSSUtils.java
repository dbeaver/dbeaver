package org.jkiss.dbeaver.ui.css;

import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.swt.widgets.Widget;

public class CSSUtils {
    private CSSUtils() {
    }

    /**
     * Set value to a widget as a CSSSWTConstants.CSS_CLASS_NAME_KEY value.
     * @param widget
     * @param value
     */
    public static void setCSSClass(Widget widget, String value){
        widget.setData(CSSSWTConstants.CSS_CLASS_NAME_KEY, value);
    }
}
