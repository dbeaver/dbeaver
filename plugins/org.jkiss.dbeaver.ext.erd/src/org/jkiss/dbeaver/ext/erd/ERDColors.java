package org.jkiss.dbeaver.ext.erd;

import org.eclipse.swt.graphics.Color;
import org.jkiss.dbeaver.ui.UIUtils;

public class ERDColors {

    public static final String[] EXTRA_BORDER_COLORS = new String[] {
            ERDConstants.COLOR_ERD_BORDERS_COLOR_1,
            ERDConstants.COLOR_ERD_BORDERS_COLOR_2,
            ERDConstants.COLOR_ERD_BORDERS_COLOR_3,
            ERDConstants.COLOR_ERD_BORDERS_COLOR_4,
            ERDConstants.COLOR_ERD_BORDERS_COLOR_5,
            ERDConstants.COLOR_ERD_BORDERS_COLOR_6,
            ERDConstants.COLOR_ERD_BORDERS_COLOR_7
    };

    public static final String[] EXTRA_HEADER_COLORS = new String[] {
            ERDConstants.COLOR_ERD_HEADER_COLOR_1,
            ERDConstants.COLOR_ERD_HEADER_COLOR_2,
            ERDConstants.COLOR_ERD_HEADER_COLOR_3,
            ERDConstants.COLOR_ERD_HEADER_COLOR_4,
            ERDConstants.COLOR_ERD_HEADER_COLOR_5,
            ERDConstants.COLOR_ERD_HEADER_COLOR_6,
            ERDConstants.COLOR_ERD_HEADER_COLOR_7
    };

    public static Color getBorderColor(int index) {
        if (index >= EXTRA_BORDER_COLORS.length) {
            index = index % EXTRA_BORDER_COLORS.length;
        }
        return UIUtils.getColorRegistry().get(EXTRA_BORDER_COLORS[index]);
    }

    public static Color getHeaderColor(int index) {
        if (index >= EXTRA_HEADER_COLORS.length) {
            index = index % EXTRA_HEADER_COLORS.length;
        }
        return UIUtils.getColorRegistry().get(EXTRA_HEADER_COLORS[index]);
    }

}
