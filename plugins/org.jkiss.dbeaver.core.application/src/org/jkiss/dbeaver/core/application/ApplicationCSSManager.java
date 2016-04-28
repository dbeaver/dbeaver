/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.core.application;


import org.eclipse.e4.ui.css.core.dom.ExtendedDocumentCSS;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.Log;
import org.w3c.dom.stylesheets.StyleSheet;
import org.w3c.dom.stylesheets.StyleSheetList;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

class ApplicationCSSManager {

    private static final Log log = Log.getLog(ApplicationCSSManager.class);
    private static final String EXTRA_CSS = "\n" +
        ".MPartStack {\n" +
//        "  swt-tab-renderer: null;\n" +
        "  swt-tab-renderer: url('bundleclass://org.jkiss.dbeaver.core.application/org.jkiss.dbeaver.core.application.ApplicationTabRenderer');\n" +
        "  border-visible: false;\n" +
//        "  swt-selected-tabs-background: #FFFFFF #ECE9D8 100%;\n" +
//        "  swt-simple: true;\n" +
//        "  swt-mru-visible: true;\n" +
        "}\n";

    static void updateApplicationCSS(Display display) {

        CSSEngine engine = WidgetElement.getEngine(display);
        if (engine == null) {
            log.error("No CSSEngine");
            return;
        }
        ExtendedDocumentCSS doc = (ExtendedDocumentCSS) engine.getDocumentCSS();
        List<StyleSheet> sheets = new ArrayList<>();
        StyleSheetList list = doc.getStyleSheets();
        for (int i = 0; i < list.getLength(); i++) {
            sheets.add(list.item(i));
        }

        try {
            Reader reader = new StringReader(EXTRA_CSS);
            sheets.add(engine.parseStyleSheet(reader));
            doc.removeAllStyleSheets();
            for (StyleSheet sheet : sheets) {
                doc.addStyleSheet(sheet);
            }
            engine.reapply();
        } catch (Exception e) {
            log.error(e);
        }
    }

}

