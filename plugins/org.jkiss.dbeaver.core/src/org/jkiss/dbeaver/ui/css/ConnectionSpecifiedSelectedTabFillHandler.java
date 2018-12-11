package org.jkiss.dbeaver.ui.css;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.e4.ui.css.swt.dom.CTabFolderElement;
import org.eclipse.e4.ui.css.swt.helpers.SWTElementHelpers;
import org.eclipse.e4.ui.css.swt.properties.custom.CSSPropertye4SelectedTabFillHandler;
import org.eclipse.e4.ui.workbench.renderers.swt.CTabRendering;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.UIUtils;
import org.w3c.dom.css.CSSValue;

public class ConnectionSpecifiedSelectedTabFillHandler extends CSSPropertye4SelectedTabFillHandler {
    public static final String COLORED_BY_CONNECTION_TYPE = "coloredByConnectionType";


    @Override
    public boolean applyCSSProperty(Object element, String property, CSSValue value, String pseudo, CSSEngine engine)
            throws Exception {

        Widget widget = SWTElementHelpers.getWidget(element);
        if (widget == null) {
            return false;
        }

        Color newColor = getCurrentConnectionColor();
        if (COLORED_BY_CONNECTION_TYPE.equals(widget.getData(CSSSWTConstants.CSS_CLASS_NAME_KEY)) && newColor != null) {
            CTabFolder nativeWidget = (CTabFolder) ((CTabFolderElement) element).getNativeWidget();
            if (nativeWidget.getRenderer() instanceof CTabRendering) {
                ((CTabRendering) nativeWidget.getRenderer()).setSelectedTabFill(newColor);
            } else {
                nativeWidget.setBackground(newColor);
            }
            return true;
        }
        return super.applyCSSProperty(element, property, value, pseudo, engine);

    }

    static Color getCurrentConnectionColor() {
        Color color = null;
        try {
            IEditorPart activeEditor = UIUtils.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            if (activeEditor instanceof DBPContextProvider) {
                DBCExecutionContext context = ((DBPContextProvider) activeEditor).getExecutionContext();
                if (context != null) {
                    color = UIUtils.getConnectionColor(context.getDataSource().getContainer()
                            .getConnectionConfiguration());
                }
            }
        } catch (Exception e) {
            // Some UI issues. Probably workbench window or page wasn't yet created
        }
        return color;
    }
}
