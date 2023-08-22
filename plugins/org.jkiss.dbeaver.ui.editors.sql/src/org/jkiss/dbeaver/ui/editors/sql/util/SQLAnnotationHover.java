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
package org.jkiss.dbeaver.ui.editors.sql.util;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLProblemAnnotation;

import java.util.Iterator;


/**
 * SQLAnnotationHover
 */
public class SQLAnnotationHover extends AbstractSQLEditorTextHover
    implements ITextHover, IAnnotationHover, ITextHoverExtension, ITextHoverExtension2 {
    private static final Log log = Log.getLog(SQLAnnotationHover.class);

    private SQLEditorBase editor;

    public SQLAnnotationHover(SQLEditorBase editor) {
        setEditor(editor);
    }

    @Override
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        Object hoverInfo2 = getHoverInfo2(textViewer, hoverRegion);
        return hoverInfo2 == null ? null : hoverInfo2.toString();
    }

    @Override
    public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
        if (!(textViewer instanceof ISourceViewer)) {
            return null;
        }
        ISourceViewer sourceViewer = (ISourceViewer) textViewer;
        for (Iterator<Annotation> ai = sourceViewer.getAnnotationModel().getAnnotationIterator(); ai.hasNext(); ) {
            Annotation anno = ai.next();
            if (isSupportedAnnotation(anno)) {
                Position annoPosition = sourceViewer.getAnnotationModel().getPosition(anno);
                if (annoPosition != null && annoPosition.overlapsWith(hoverRegion.getOffset(), 1)) {
                    return anno.getText();
                }
            }
        }

        return null;
    }

    private boolean isSupportedAnnotation(Annotation anno) {
        return anno instanceof SpellingAnnotation || anno instanceof SQLProblemAnnotation;
    }

    @Override
    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
        if (!(textViewer instanceof ISourceViewer)) {
            return null;
        }
        IAnnotationModel annotationModel = ((ISourceViewer) textViewer).getAnnotationModel();
        if (annotationModel != null) {
            for (Iterator<Annotation> ai = annotationModel.getAnnotationIterator(); ai.hasNext(); ) {
                Annotation anno = ai.next();
                if (isSupportedAnnotation(anno)) {
                    Position annoPosition = annotationModel.getPosition(anno);
                    if (annoPosition != null && annoPosition.overlapsWith(offset, 1)) {
                        return new Region(annoPosition.getOffset(), annoPosition.getLength());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Show info from annotations on the specified line
     */
    @Override
    public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber) {
        try {
            int linePosition = sourceViewer.getDocument().getLineOffset(lineNumber);
            int lineLength = sourceViewer.getDocument().getLineLength(lineNumber);
            for (Iterator<Annotation> ai = sourceViewer.getAnnotationModel().getAnnotationIterator(); ai.hasNext(); ) {
                Annotation anno = ai.next();
                if (isSupportedAnnotation(anno)) {
                    Position annoPosition = sourceViewer.getAnnotationModel().getPosition(anno);
                    if (annoPosition != null && annoPosition.overlapsWith(linePosition, lineLength)) {
                        return anno.getText();
                    }
                }
            }
        } catch (BadLocationException e) {
            log.debug(e);
        }
        return null;
    }

    @Override
    public void setEditor(IEditorPart editor) {
        this.editor = (SQLEditorBase) editor;
    }

    public IInformationControlCreator getHoverControlCreator() {
        return parent -> {
            DefaultInformationControl control = new DefaultInformationControl(parent, false);
            control.setSizeConstraints(60, 10);
            return control;
        };
    }

}