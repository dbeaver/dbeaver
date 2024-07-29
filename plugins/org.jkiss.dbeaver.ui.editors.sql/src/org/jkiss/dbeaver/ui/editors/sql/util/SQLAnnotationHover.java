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
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.UIUtils;
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

    @Override
    public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
        if (!(textViewer instanceof ISourceViewer)) {
            return null;
        }
        List<IHyperlink> links = new ArrayList<>();
        ISourceViewer sourceViewer = (ISourceViewer) textViewer;
        for (Iterator<Annotation> ai = sourceViewer.getAnnotationModel().getAnnotationIterator(); ai.hasNext(); ) {
            Annotation anno = ai.next();
            if (isSupportedAnnotation(anno)) {
                Position annoPosition = sourceViewer.getAnnotationModel().getPosition(anno);
                if (annoPosition != null) {
                    if (annoPosition.overlapsWith(hoverRegion.getOffset(), hoverRegion.getLength())) {
                        links.add(new IHyperlink() {
                            @Override
                            public IRegion getHyperlinkRegion() {
                                return new Region(annoPosition.getOffset(), annoPosition.getLength());
                            }

                            @Override
                            public String getTypeLabel() {
                                return null;
                            }

                            @Override
                            public String getHyperlinkText() {
                                return anno.getText();
                            }

                            @Override
                            public void open() {
                                TextViewer textViewer = editor.getTextViewer();
                                if (textViewer != null) {
                                    textViewer.setSelectedRange(annoPosition.getOffset(), annoPosition.getLength());
                                    textViewer.revealRange(annoPosition.getOffset(), annoPosition.getLength());
                                }
                            }
                        });
                    }
                }
            }
        }
        links.sort(Comparator.comparingInt(l -> l.getHyperlinkRegion().getOffset()));
        return links.isEmpty() ? null : links.toArray(IHyperlink[]::new);
    }

    @Override
    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
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
            IRegion lineRegion = sourceViewer.getDocument().getLineInformation(lineRange.getStartLine());
            return this.getHoverInfo2(sourceViewer, lineRegion);
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

        private Composite linksContainer;
        private int firstLinkOffset = -1;

        public LinkListInformationControl(Shell parentShell) {
            super(parentShell, false);
            this.setBackgroundColor(new Color(255, 128, 128));
            create();
        }

        @Override
        public void setInformation(String information) {
            //replaced by IInformationControlExtension2#setInput(java.lang.Object)
        }

        @Override
        public void setInput(Object input) {
            for (IHyperlink hyperlink : (IHyperlink[]) input) {
                if (this.firstLinkOffset < 0) {
                    firstLinkOffset = hyperlink.getHyperlinkRegion().getOffset();
                }
                Hyperlink link = new Hyperlink(this.linksContainer, SWT.NONE);
                link.setText(hyperlink.getHyperlinkText());
                link.setUnderlined(true);
                link.addHyperlinkListener(new IHyperlinkListener() {
                    private Point oldSelection = null;

                    @Override
                    public void linkEntered(HyperlinkEvent e) {
                        oldSelection = editor.getTextViewer() != null ? editor.getTextViewer().getSelectedRange() : null;
                        IRegion hyperlinkRegion = hyperlink.getHyperlinkRegion();
                        editor.getTextViewer().setSelectedRange(hyperlinkRegion.getOffset(), hyperlinkRegion.getLength());
                    }

                    @Override
                    public void linkExited(HyperlinkEvent e) {
                        if (oldSelection != null && editor.getTextViewer() != null) {
                            editor.getTextViewer().setSelectedRange(oldSelection.x, oldSelection.y);
                        }
                    }

                    @Override
                    public void linkActivated(HyperlinkEvent e) {
                        hyperlink.open();
                    }
                });
            }
            super.getShell().pack(true);
        }

        @Override
        protected void createContent(Composite parent) {
            this.linksContainer = UIUtils.createComposite(parent, 1);
            this.linksContainer.setLayout(GridLayoutFactory.swtDefaults().create());
        }

        @Override
        public boolean hasContents() {
            return true;
        }

        @Override
        public Point computeSizeHint() {
            Rectangle bounds = this.getBounds();
            return new Point(bounds.width, bounds.height);
        }

        @Override
        public void setVisible(boolean visible) {
            if (visible && this.firstLinkOffset >= 0 && editor.getDocument() != null && editor.getTextViewer() != null) {
                IRegion modelLineRange;
                try {
                    modelLineRange = editor.getDocument().getLineInformationOfOffset(this.firstLinkOffset);
                    IRegion visualLineRange = editor.getTextViewer().modelRange2WidgetRange(modelLineRange);
                    StyledText widget = editor.getTextViewer().getTextWidget();
                    int offset = visualLineRange.getOffset();
                    Rectangle localLineBounds =  widget.getTextBounds(offset, offset + visualLineRange.getLength() - 1);
                    Rectangle globalLineBounds = Geometry.toDisplay(widget, localLineBounds);

                    if (this.getBounds().intersects(globalLineBounds)) {
                        this.setLocation(new Point(this.getBounds().x, globalLineBounds.y + globalLineBounds.height));
                    }
                    super.getShell().pack(true);
                } catch (BadLocationException e) {
                    // nah, no way to adjust position
                }
            }
            super.setVisible(visible);
        }
    }

}