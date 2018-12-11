package org.jkiss.dbeaver.ui.css;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.properties.AbstractCSSPropertySWTHandler;
import org.eclipse.swt.widgets.Control;
import org.w3c.dom.css.CSSValue;
@Deprecated
public class EditorItemElementHandler extends AbstractCSSPropertySWTHandler {

	public EditorItemElementHandler() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void applyCSSProperty(Control control, String property, CSSValue value, String pseudo, CSSEngine engine) throws Exception {

	}

	@Override
	protected String retrieveCSSProperty(Control control, String property, String pseudo, CSSEngine engine) throws Exception {
		return null;
	}

}
