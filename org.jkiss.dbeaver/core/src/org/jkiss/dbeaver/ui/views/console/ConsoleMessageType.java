package org.jkiss.dbeaver.ui.views.console;

import org.eclipse.swt.SWT;

/**
 * ConsoleMessageType
 *
 * @author Serge Rieder
 */
public enum ConsoleMessageType {
    INFO(SWT.COLOR_WIDGET_FOREGROUND),
    CODE(SWT.COLOR_DARK_BLUE),
    COMMENT(SWT.COLOR_GRAY),
    ERROR(SWT.COLOR_RED);

    private final int color;

    public int getColor()
    {
        return color;
    }

    ConsoleMessageType(int color)
    {
        this.color = color;
    }
}
