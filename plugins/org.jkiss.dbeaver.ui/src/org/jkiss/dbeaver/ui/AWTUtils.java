package org.jkiss.dbeaver.ui;

import java.awt.*;

public class AWTUtils {

    public static java.awt.Color makeAWTColor(org.eclipse.swt.graphics.Color src) {
        org.eclipse.swt.graphics.RGB swtBgColor = src.getRGB();
        return new Color(swtBgColor.red, swtBgColor.green, swtBgColor.blue);
    }

}
