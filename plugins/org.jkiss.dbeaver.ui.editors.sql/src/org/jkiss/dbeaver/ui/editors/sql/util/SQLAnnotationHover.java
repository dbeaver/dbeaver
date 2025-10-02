/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.antlr.v4.runtime.misc.Interval;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLSemanticErrorAnnotation;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLProblemAnnotation;

import java.util.*;


/**
 * SQLAnnotationHover
 */
public class SQLAnnotationHover extends AbstractSQLEditorTextHover
    implements ITextHover, IAnnotationHover, ITextHoverExtension, ITextHoverExtension2, IAnnotationHoverExtension {
    private static final Log log = Log.getLog(SQLAnnotationHover.class);

    private SQLEditorBase editor;

    public SQLAnnotationHover(SQLEditorBase editor) {
        setEditor(editor);
    }

    /**
     * Show info from annotations on the specified line
     */
    @Override
    public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber) {
        try {
            int linePosition = sourceViewer.getDocument().getLineOffset(lineNumber);
            int lineLength = sourceViewer.getDocument().getLineLength(lineNumber);
            StringBuilder sb = new StringBuilder();
            for (Iterator<Annotation> ai = sourceViewer.getAnnotationModel().getAnnotationIterator(); ai.hasNext(); ) {
                Annotation anno = ai.next();
                if (isSupportedAnnotation(anno)) {
                    Position annoPosition = sourceViewer.getAnnotationModel().getPosition(anno);
                    if (annoPosition != null) {
                        if (annoPosition.overlapsWith(linePosition, lineLength)) {
                            sb.append(anno.getText()).append("; ");
                        }
                    }
                }
            }
            return sb.isEmpty() ? null : sb.toString();
        } catch (BadLocationException e) {
            log.debug(e);
            return null;
        }
    }

    @Override
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        Object hoverInfo2 = getHoverInfo2(textViewer, hoverRegion);
        return hoverInfo2 == null ? null : hoverInfo2.toString();
    }

    @Nullable
    @Override
    public Object getHoverInfo2(
        @NotNull ITextViewer textViewer,
        @NotNull IRegion hoverRegion
    ) {
        return this.getAnnotationsHoverInfo(textViewer, hoverRegion, null, false);
    }

    @Nullable
    public AnnotationsInformationView.AnnotationsHoverInfo getAnnotationsHoverInfo(
        @NotNull ITextViewer textViewer,
        @NotNull IRegion hoverRegion,
        @Nullable Integer anchorLine,
        boolean adjustPosition
    ) {
        if (!(textViewer instanceof ISourceViewer)) {
            return null;
        }
        Map<String, AnnotationsInformationView.AnnotationsGroupInfo> linkGroupsByMessage = new HashMap<>();
        IAnnotationModel annotationModel = ((ISourceViewer) textViewer).getAnnotationModel();
        for (Iterator<Annotation> ai = annotationModel.getAnnotationIterator(); ai.hasNext(); ) {
            Annotation anno = ai.next();
            if (isSupportedAnnotation(anno)) {
                Position annoPosition = annotationModel.getPosition(anno);
                if (annoPosition != null) {
                    if (annoPosition.overlapsWith(hoverRegion.getOffset(), hoverRegion.getLength())) {
                        linkGroupsByMessage.computeIfAbsent(anno.getText(), AnnotationsInformationView.AnnotationsGroupInfo::new).add(anno, annoPosition);
                    }
                }
            }
        }

        if (linkGroupsByMessage.isEmpty()) {
            return null;
        } else {
            List<AnnotationsInformationView.AnnotationsGroupInfo> annotationsGroups = linkGroupsByMessage.values().stream()
                .sorted(Comparator.comparing(g -> g.getFirstPosition().getOffset()))
                .toList();

            if (anchorLine == null) {
                Position lastAnnotationPos = annotationsGroups.stream().max(Comparator.comparing(
                    g -> g.getLastPosition().getOffset() + g.getLastPosition().getLength()
                )).get().getLastPosition();
                int lastAnnotationOffset = lastAnnotationPos.getOffset() + lastAnnotationPos.getLength();
                try {
                    anchorLine = editor.getDocument().getLineOfOffset(lastAnnotationOffset);
                } catch (BadLocationException e) {
                    log.debug("Error obtaining anchor line of annotation offset " + lastAnnotationPos, e);
                    try {
                        anchorLine = editor.getDocument().getLineOfOffset(hoverRegion.getOffset());
                    } catch (BadLocationException ex) {
                        log.debug("Error obtaining anchor line of hover region offset " + hoverRegion.getOffset(), e);
                        anchorLine = -1;
                    }
                }
            }

            return new AnnotationsInformationView.AnnotationsHoverInfo(annotationsGroups, adjustPosition ? hoverRegion : null, anchorLine);
        }
    }

    @Nullable
    @Override
    public IRegion getHoverRegion(@NotNull ITextViewer textViewer, int offset) {
        if (!(textViewer instanceof ISourceViewer)) {
            return null;
        }
        Interval hoverInterval = new Interval(offset, offset);
        Interval resultInterval = null;
        IAnnotationModel annotationModel = ((ISourceViewer) textViewer).getAnnotationModel();
        if (annotationModel != null) {
            for (Iterator<Annotation> ai = annotationModel.getAnnotationIterator(); ai.hasNext(); ) {
                Annotation anno = ai.next();
                if (isSupportedAnnotation(anno)) {
                    Position annoPosition = annotationModel.getPosition(anno);
                    if (annoPosition != null) {
                        Interval annoInterval = new Interval(annoPosition.getOffset(), annoPosition.getOffset() + annoPosition.getLength());
                        if (annoInterval.properlyContains(hoverInterval)) {
                            resultInterval = resultInterval == null ? annoInterval : resultInterval.union(annoInterval);
                        }
                    }
                }
            }
        }
        return resultInterval == null ? null : new Region(resultInterval.a, resultInterval.length());
    }

    @Override
    public Object getHoverInfo(ISourceViewer sourceViewer, ILineRange lineRange, int visibleNumberOfLines) {
        try {
            Integer anchorLine = lineRange.getNumberOfLines() == 1 ? lineRange.getStartLine() : null;
            IRegion lineRegion = sourceViewer.getDocument().getLineInformation(lineRange.getStartLine());
            return this.getAnnotationsHoverInfo(sourceViewer, lineRegion, anchorLine, false);
        } catch (BadLocationException e) {
            log.debug(e);
            return null;
        }
    }

    @Override
    public ILineRange getHoverLineRange(ISourceViewer viewer, int lineNumber) {
        return new LineRange(lineNumber, 1);
    }

    @Override
    public boolean canHandleMouseCursor() {
        return true;
    }

    @Override
    public void setEditor(IEditorPart editor) {
        this.editor = (SQLEditorBase) editor;
    }

    public IInformationControlCreator getHoverControlCreator() {
        return LinkListInformationControl::new;
    }

    private boolean isSupportedAnnotation(Annotation anno) {
        return anno instanceof SpellingAnnotation || anno instanceof SQLProblemAnnotation || anno instanceof SQLSemanticErrorAnnotation;
    }

    private class LinkListInformationControl extends AbstractInformationControl implements IInformationControlExtension2 {
        @NotNull
        private final AnnotationsInformationView infoView;

        public LinkListInformationControl(@Nullable Shell parentShell) {
            super(parentShell, false);
            this.setBackgroundColor(new Color(255, 128, 128));
            this.infoView = new AnnotationsInformationView(this, editor);
            create();
        }

        @Override
        public IInformationControlCreator getInformationPresenterControlCreator() {
            // Fix problem annotation tooltip disappear on mouse move/hover
            return LinkListInformationControl::new;
        }

        @Override
        public void setInformation(String information) {
            //replaced by IInformationControlExtension2#setInput(java.lang.Object)
        }

        @Override
        public void setInput(@NotNull Object input) {
            this.infoView.setLinksInformation((AnnotationsInformationView.AnnotationsHoverInfo) input);
        }

        @Override
        protected void createContent(@NotNull Composite parent) {
            this.infoView.createControl(parent);
        }

        @Override
        public boolean hasContents() {
            return true;
        }

        @NotNull
        @Override
        public Point computeSizeHint() {
            Rectangle bounds = this.getBounds();
            return new Point(bounds.width, bounds.height);
        }

        @Override
        public void setVisible(boolean visible) {
            if (visible) {
                this.infoView.show();
            }
            super.setVisible(visible);
        }
    }
}