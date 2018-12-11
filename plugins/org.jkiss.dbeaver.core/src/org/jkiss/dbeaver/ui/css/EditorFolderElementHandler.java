package org.jkiss.dbeaver.ui.css;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.e4.ui.css.swt.properties.custom.CSSPropertySelectedTabsSWTHandler;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;
import org.w3c.dom.css.CSSValue;

/**
 * Editor tab colorer.
 * IT is disabled because it breaks dark theme.
 */
@Deprecated
public class EditorFolderElementHandler extends CSSPropertySelectedTabsSWTHandler {

	private static final String PROP_BACKGROUND = "swt-selected-tabs-background";

	public EditorFolderElementHandler() {
	}

	@Override
	protected void applyCSSProperty(Control control, String property, CSSValue value, String pseudo, CSSEngine engine) throws Exception {
		if (control instanceof CTabFolder) {
			Object cssClass = control.getData(CSSSWTConstants.CSS_CLASS_NAME_KEY);
			if (CommonUtils.toString(cssClass).contains("EditorStack")) {
				//Object cssId = control.getData(CSSSWTConstants.CSS_ID_KEY);
				if (PROP_BACKGROUND.equalsIgnoreCase(property) && (value.getCssValueType() == CSSValue.CSS_VALUE_LIST)) {
					Color newColor = null;
					try {
						IEditorPart activeEditor = UIUtils.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
						if (activeEditor instanceof DBPContextProvider) {
                            DBCExecutionContext context = ((DBPContextProvider) activeEditor).getExecutionContext();
                            if (context != null) {
                                newColor = UIUtils.getConnectionColor(context.getDataSource().getContainer().getConnectionConfiguration());
                            }
                        }
					} catch (Exception e) {
						// Some UI issues. Probably workbench window or page wasn't yet created
					}
					if (newColor == null) {
						super.applyCSSProperty(control, property, value, pseudo, engine);
					} else {
						((CTabFolder) control).setSelectionBackground(new Color[] {newColor, newColor}, new int[] {100}, true);
						//((CTabFolder) control).setSelectionBackground(newColor);
					}
				}
			}
		}
	}

	@Override
	protected String retrieveCSSProperty(Control control, String property, String pseudo, CSSEngine engine) throws Exception {
		return null;
	}

}
