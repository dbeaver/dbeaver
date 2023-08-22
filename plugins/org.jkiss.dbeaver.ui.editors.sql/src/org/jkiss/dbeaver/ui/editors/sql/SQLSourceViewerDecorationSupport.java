/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.source.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.jkiss.dbeaver.ui.UIStyles;

public class SQLSourceViewerDecorationSupport extends SourceViewerDecorationSupport {

    private AnnotationPainter annotationPainter;

    public SQLSourceViewerDecorationSupport(
        ISourceViewer sourceViewer,
        IOverviewRuler overviewRuler,
        IAnnotationAccess annotationAccess,
        ISharedTextColors sharedTextColors
    ) {
        super(sourceViewer, overviewRuler, annotationAccess, sharedTextColors);
    }

    @Override
    public void install(IPreferenceStore store) {
        super.install(store);
        setSpellingAnnotationsStyle();
    }

    protected AnnotationPainter createAnnotationPainter() {
        annotationPainter = super.createAnnotationPainter();
        setSpellingAnnotationsStyle();
        return annotationPainter;
    }

    private void setSpellingAnnotationsStyle() {
        // Set spelling annotation color to shadow
        annotationPainter.setAnnotationTypeColor(
            SpellingAnnotation.TYPE,
            Display.getDefault().getSystemColor(UIStyles.isDarkTheme() ?
                SWT.COLOR_WIDGET_LIGHT_SHADOW :
                SWT.COLOR_WIDGET_NORMAL_SHADOW));
    }

}
