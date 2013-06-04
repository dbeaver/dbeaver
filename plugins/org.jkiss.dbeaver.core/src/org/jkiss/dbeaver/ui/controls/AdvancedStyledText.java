package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;

/**
 * Advanced styled text control
 */
public class AdvancedStyledText {

    public static StyledText createControl(Composite parent, int style)
    {
        StyledText text = new StyledText(parent, style);

        return text;
    }

}
