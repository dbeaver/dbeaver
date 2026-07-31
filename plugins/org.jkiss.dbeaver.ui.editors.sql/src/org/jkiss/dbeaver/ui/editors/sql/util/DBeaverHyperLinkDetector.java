/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.util;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.URLHyperlinkDetector;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;

import java.util.ArrayList;

public class DBeaverHyperLinkDetector extends URLHyperlinkDetector {
    private static final Log log = Log.getLog(DBeaverHyperLinkDetector.class);

    @Override
    @Nullable
    public IHyperlink[] detectHyperlinks(@NotNull ITextViewer textViewer, @NotNull IRegion region, boolean canShowMultipleHyperlinks) {
        IHyperlink[] hyperlinks = super.detectHyperlinks(textViewer, region, canShowMultipleHyperlinks);
        if (hyperlinks == null || hyperlinks.length == 0) {
            return null;
        }
        ArrayList<IHyperlink> validHyperLinks = new ArrayList<>();
        for (IHyperlink hyperlink : hyperlinks) {
            IRegion hyperlinkRegion = hyperlink.getHyperlinkRegion();
            try {
                String link = textViewer.getDocument().get(hyperlinkRegion.getOffset(), hyperlinkRegion.getLength());
                if (!link.startsWith("file:")) {
                    validHyperLinks.add(hyperlink);
                }
            } catch (BadLocationException e) {
                log.error("Error detecting hyperlink", e);
            }
        }
        if (validHyperLinks.isEmpty()) {
            return null;
        }
        return validHyperLinks.toArray(new IHyperlink[0]);
    }
}
