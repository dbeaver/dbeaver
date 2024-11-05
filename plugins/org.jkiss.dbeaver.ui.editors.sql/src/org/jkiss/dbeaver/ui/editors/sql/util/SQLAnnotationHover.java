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
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.text.*;
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
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.stm.STMUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLSemanticErrorAnnotation;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLProblemAnnotation;
import org.jkiss.utils.CommonUtils;

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
        return getHoverInfoImpl(textViewer, hoverRegion, null);
    }

    private Object getHoverInfoImpl(ITextViewer textViewer, IRegion hoverRegion, Integer anchorLine) {
        if (!(textViewer instanceof ISourceViewer)) {
            return null;
        }
        Map<String, AnnotationsGroupInfo> linkGroupsByMessage = new HashMap<>();
        IAnnotationModel annotationModel = ((ISourceViewer) textViewer).getAnnotationModel();
        for (Iterator<Annotation> ai = annotationModel.getAnnotationIterator(); ai.hasNext(); ) {
            Annotation anno = ai.next();
            if (isSupportedAnnotation(anno)) {
                Position annoPosition = annotationModel.getPosition(anno);
                if (annoPosition != null) {
                    if (annoPosition.overlapsWith(hoverRegion.getOffset(), hoverRegion.getLength())) {
                        linkGroupsByMessage.computeIfAbsent(anno.getText(), AnnotationsGroupInfo::new).add(anno, annoPosition);
                    }
                }
            }
        }

        if (linkGroupsByMessage.isEmpty()) {
            return null;
        } else {
            List<AnnotationsGroupInfo> annotationsGroups = linkGroupsByMessage.values().stream()
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
            return new AnnotationsHoverInfo(annotationsGroups, anchorLine);
        }
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
            Integer anchorLine = lineRange.getNumberOfLines() == 1 ? lineRange.getStartLine() : null;
            IRegion lineRegion = sourceViewer.getDocument().getLineInformation(lineRange.getStartLine());
            return this.getHoverInfoImpl(sourceViewer, lineRegion, anchorLine);
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

        private IHyperlinkListener hyperlinkListener = new IHyperlinkListener() {
            private Point oldSelection = null;

            @Override
            public void linkEntered(HyperlinkEvent e) {
                if (e.getHref() instanceof AnnotationHyperlinkInfo hyperlink) {
                    this.oldSelection = editor.getTextViewer() != null ? editor.getTextViewer().getSelectedRange() : null;
                    Position hyperlinkRegion = hyperlink.getPosition();
                    if (!hyperlinkRegion.isDeleted) {
                        editor.getTextViewer().setSelectedRange(hyperlinkRegion.getOffset(), hyperlinkRegion.getLength());
                    }
                }
            }

            @Override
            public void linkExited(HyperlinkEvent e) {
                if (this.oldSelection != null && editor.getTextViewer() != null) {
                    editor.getTextViewer().setSelectedRange(this.oldSelection.x, this.oldSelection.y);
                    this.oldSelection = null;
                }
            }

            @Override
            public void linkActivated(HyperlinkEvent e) {
                if (e.getHref() instanceof AnnotationHyperlinkInfo hyperlink) {
                    hyperlink.open();
                }
            }
        };

        private Composite linksContainer;
        private int tooltipAnchorLine = -1;

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
            AnnotationsHoverInfo hoverInfo = (AnnotationsHoverInfo) input;
            this.tooltipAnchorLine = hoverInfo.tooltipAnchorLine;
            for (AnnotationsGroupInfo annotationGroup : hoverInfo.annotationsGroups()) {
                Composite linksGroupContainer;
                if (hoverInfo.annotationsGroups().size() > 1) {
                    linksGroupContainer = UIUtils.createComposite(linksContainer, 2);
                    DBIcon icon = annotationGroup.getIcon();
                    if (icon != null) {
                        UIUtils.createLabel(linksGroupContainer, icon);
                    } else {
                        UIUtils.createPlaceholder(linksGroupContainer, 1);
                    }
                } else {
                    linksGroupContainer = this.linksContainer;
                }
                List<AnnotationHyperlinkInfo> hyperlinks = annotationGroup.getAnnotations();
                assert hyperlinks.size() > 0;
                final int alsoLinksLimit = 5;
                final int alsoLinksToShow = Math.min(alsoLinksLimit, hyperlinks.size() - 1) + 1;
                Composite groupLinksContainer = UIUtils.createPlaceholder(linksGroupContainer, hyperlinks.size() == 1 ? 1 : (alsoLinksToShow * 2 + 1), 0);
                this.createHyperlinkControl(groupLinksContainer, hyperlinks.get(0), annotationGroup.getMessage());
                if (hyperlinks.size() > 2) {
                    UIUtils.createLabel(groupLinksContainer, " (also at position ");
                    for (int i = 1; i < alsoLinksToShow; i++) {
                        AnnotationHyperlinkInfo hyperlink = hyperlinks.get(i);
                        if (i > 1) {
                            UIUtils.createLabel(groupLinksContainer, ", ");
                        }
                        this.createHyperlinkControl(groupLinksContainer, hyperlink, Integer.toString(hyperlink.getPosition().getOffset()));
                    }
                    UIUtils.createLabel(groupLinksContainer, hyperlinks.size() <= alsoLinksLimit ? ")" : (", ... " + hyperlinks.size() + " such problems in line)"));
                }
            }
            super.getShell().pack(true);
        }

        private void createHyperlinkControl(Composite groupLinksContainer, AnnotationHyperlinkInfo hyperlink, String text) {
            Hyperlink link = new Hyperlink(groupLinksContainer, SWT.NONE);
            link.setHref(hyperlink);
            link.setText(text);
            link.setUnderlined(true);
            link.addHyperlinkListener(this.hyperlinkListener);
            if (hyperlink.getAnnotation() instanceof SQLSemanticErrorAnnotation s) {
                String underlyingError = s.getUnderlyingErrorMessage();
                if (CommonUtils.isNotEmpty(underlyingError)) {
                    link.setToolTipText(underlyingError);
                }
            }
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
            if (visible && this.tooltipAnchorLine >= 0 && editor.getDocument() != null && editor.getTextViewer() != null) {
                try {
                    IRegion modelLineRange = editor.getDocument().getLineInformation(this.tooltipAnchorLine);
                    IRegion visualLineRange = editor.getTextViewer().modelRange2WidgetRange(modelLineRange);
                    StyledText widget = editor.getTextViewer().getTextWidget();
                    int offset = visualLineRange.getOffset();
                    Rectangle localLineBounds =  widget.getTextBounds(offset, offset + visualLineRange.getLength() - 1);
                    Rectangle globalLineBounds = Geometry.toDisplay(widget, localLineBounds);
                    Rectangle globalWidgetBounds = Geometry.toDisplay(widget, widget.getBounds());

                    int y = Math.min(
                        globalLineBounds.y + globalLineBounds.height,
                        globalWidgetBounds.y + globalWidgetBounds.height - widget.getHorizontalBar().getSize().y
                    );
                    boolean hasTooltipRanAway = !globalWidgetBounds.intersects(this.getBounds());

                    Rectangle adjustedBounds = new Rectangle(globalLineBounds.x, y, globalLineBounds.width, globalLineBounds.height);
                    if (this.getBounds().intersects(adjustedBounds) || hasTooltipRanAway) {
                        int x = hasTooltipRanAway
                            ? Math.min(widget.getDisplay().getCursorLocation().x, globalLineBounds.x)
                            : this.getBounds().x;
                        this.setLocation(new Point(x, y));
                    }
                    super.getShell().pack(true);
                } catch (BadLocationException e) {
                    // nah, no way to adjust position
                }
            }
            super.setVisible(visible);
        }
    }

    private class AnnotationHyperlinkInfo {
        @NotNull
        private final Annotation annotation;
        @NotNull
        private final Position position;

        private AnnotationHyperlinkInfo(@NotNull Annotation annotation, @NotNull Position position) {
            this.annotation = annotation;
            this.position = position;
        }

        @NotNull
        public Annotation getAnnotation() {
            return annotation;
        }

        @NotNull
        public Position getPosition() {
            return this.position;
        }

        public void open() {
            TextViewer textViewer = editor.getTextViewer();
            if (textViewer != null && !this.position.isDeleted) {
                textViewer.setSelectedRange(this.position.getOffset(), this.position.getLength());
                textViewer.revealRange(this.position.getOffset(), this.position.getLength());
            }
        }
    }

    private class AnnotationsGroupInfo {
        private static final Position MIN_POSITION = new Position(0, 0);
        private static final Position MAX_POSITION = new Position(Integer.MAX_VALUE, 0);

        private static final int UNKNOWN_SEVERITY = -1;

        @NotNull
        private final List<AnnotationHyperlinkInfo> annotations = new ArrayList<>();
        @NotNull
        private final String message;
        @NotNull
        private Position firstPosition = MAX_POSITION;
        @NotNull
        private Position lastPosition = MIN_POSITION;

        private int severity = UNKNOWN_SEVERITY;

        private AnnotationsGroupInfo(@NotNull String message) {
            this.message = message;
        }

        public void add(@NotNull Annotation anno, @NotNull Position annoPosition) {
            Position firstPos = this.firstPosition;
            if (firstPos.offset > annoPosition.offset) {
                this.firstPosition = annoPosition;
            }
            Position lastPos = this.lastPosition;
            if (lastPos.offset + lastPos.length < annoPosition.offset + annoPosition.length) {
                this.lastPosition = annoPosition;
            }
            AnnotationHyperlinkInfo entry = new AnnotationHyperlinkInfo(anno, annoPosition);
            STMUtils.orderedInsert(this.annotations, e -> e.getPosition().getOffset(), entry, Integer::compare);
            this.severity = Math.max(this.severity, getAnnotationSeverity(anno));
        }

        @NotNull
        public List<AnnotationHyperlinkInfo> getAnnotations() {
            return this.annotations;
        }

        @NotNull
        public String getMessage() {
            return this.message;
        }

        @NotNull
        public Position getFirstPosition() {
            return this.firstPosition;
        }

        @NotNull
        public Position getLastPosition() {
            return this.lastPosition;
        }

        @Nullable
        public DBIcon getIcon() {
            return switch (this.severity) {
                case IMarker.SEVERITY_ERROR -> DBIcon.SMALL_ERROR;
                case IMarker.SEVERITY_WARNING -> DBIcon.SMALL_WARNING;
                case IMarker.SEVERITY_INFO -> DBIcon.SMALL_INFO;
                default -> null;
            };
        }

        private int getAnnotationSeverity(@NotNull Annotation anno) {
            if (anno instanceof MarkerAnnotation ma) {
                try {
                    return ma.getMarker().getAttribute(IMarker.SEVERITY) instanceof Integer n ? n : IMarker.SEVERITY_INFO;
                } catch (CoreException e) {
                    log.error("Failed to obtain annotation severity icon", e);
                    return UNKNOWN_SEVERITY;
                }
            } else { // marker-less annotation
                return IMarker.SEVERITY_INFO;
            }
        }
    }

    private record AnnotationsHoverInfo(@NotNull List<AnnotationsGroupInfo> annotationsGroups, int tooltipAnchorLine) {
    }
}