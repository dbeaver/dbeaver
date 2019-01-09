package org.jkiss.dbeaver.ui.css;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.e4.ui.css.swt.dom.CTabFolderElement;
import org.eclipse.e4.ui.css.swt.helpers.SWTElementHelpers;
import org.eclipse.e4.ui.css.swt.properties.custom.CSSPropertyUnselectedTabsSWTHandler;
import org.eclipse.e4.ui.workbench.renderers.swt.CTabRendering;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Widget;
import org.w3c.dom.css.CSSValue;

/**
 * Needed to override theme styles.
 * For now it's used only for coloring widgets regarding the connection type color.
 */
public class CustomUnselectedTabHandler extends CSSPropertyUnselectedTabsSWTHandler {

    @Override
    public boolean applyCSSProperty(Object element, String property, CSSValue value, String pseudo, CSSEngine engine) throws Exception {
        Widget widget = SWTElementHelpers.getWidget(element);
        if (widget == null) {
            return false;
        }

        Color newColor = CustomSelectedTabFillHandler.getCurrentConnectionColor();
        if (DBStyles.COLORED_BY_CONNECTION_TYPE.equals(widget.getData(CSSSWTConstants.CSS_CLASS_NAME_KEY)) && newColor != null) {
            CTabFolder nativeWidget = (CTabFolder) ((CTabFolderElement) element).getNativeWidget();
            if (nativeWidget.getRenderer() instanceof CTabRendering) {
                ((CTabRendering) nativeWidget.getRenderer()).setUnselectedTabsColor(newColor);
            } else {
                nativeWidget.setBackground(newColor);
            }
            nativeWidget.setBackground(newColor);
            return true;
        }
        return super.applyCSSProperty(element, property, value, pseudo, engine);
    }
}
