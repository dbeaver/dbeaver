/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    private static final String TABBED_FOLDER_STYLE = "TabbedFolderList { background-color: inherit; }";

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
            Reader reader = new StringReader(TABBED_FOLDER_STYLE);
            sheets.add(engine.parseStyleSheet(reader));
            doc.removeAllStyleSheets();
            for (StyleSheet sheet : sheets) {
                doc.addStyleSheet(sheet);
            }
            //engine.reapply();
        } catch (Exception e) {
            log.error(e);
        }
    }

}

