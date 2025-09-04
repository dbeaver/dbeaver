/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.eclipse.core.commands.Command;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.RowLayoutFactory;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.stm.STMUtils;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLSemanticErrorAnnotation;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class AnnotationsInformationView {

    private static final Log log = Log.getLog(AnnotationsInformationView.class);

    private static final String QUICK_FIX_COMMAND_ID = "org.eclipse.jdt.ui.edit.text.java.correction.assist.proposals";

    private Composite linksContainer;
    private int tooltipAnchorLine = -1;
    private IRegion tooltipAnchorRegion = null;
    private boolean forceAnnotationIcon = false;

    @NotNull
    private final AbstractInformationControl container;
    @NotNull
    private final SQLEditorBase editor;
    @Nullable
    private final IQuickAssistAssistant quickAssistAssistant;
    @Nullable
    private final QuickFixCommandInfo quickFixCommandInfo;

    private abstract class ContextfulHyperlinkListener implements IHyperlinkListener {
        private Point oldSelection = null;

        @Override
        public final void linkEntered(@NotNull HyperlinkEvent e) {
            if (e.getHref() instanceof AnnotationHyperlinkInfo hyperlink) {
                this.oldSelection = editor.getTextViewer() != null ? editor.getTextViewer().getSelectedRange() : null;
                Position hyperlinkRegion = hyperlink.position();
                if (!hyperlinkRegion.isDeleted) {
                    editor.getTextViewer().setSelectedRange(hyperlinkRegion.getOffset(), hyperlinkRegion.getLength());
                }
            }
        }

        @Override
        public final void linkExited(@NotNull HyperlinkEvent e) {
            if (this.oldSelection != null && editor.getTextViewer() != null) {
                editor.getTextViewer().setSelectedRange(this.oldSelection.x, this.oldSelection.y);
                this.oldSelection = null;
            }
        }

        @Override
        public final void linkActivated(HyperlinkEvent e) {
            if (e.getHref() instanceof AnnotationHyperlinkInfo hyperlink) {
                this.navigateLinkInternal(hyperlink);
            }
        }

        private void navigateLinkInternal(AnnotationHyperlinkInfo hyperlink) {
            this.oldSelection = null;
            this.navigateLink(hyperlink);
        }

        abstract void navigateLink(AnnotationHyperlinkInfo hyperlink);
    }

    @NotNull
    private final IHyperlinkListener hyperlinkListener = new ContextfulHyperlinkListener() {
        @Override
        void navigateLink(AnnotationHyperlinkInfo hyperlink) {
            hyperlink.open(editor);
        }
    };

    @NotNull
    private final IHyperlinkListener fixHyperlinkListener = new ContextfulHyperlinkListener() {
        @Override
        void navigateLink(AnnotationHyperlinkInfo hyperlink) {
            hyperlink.quickFix(editor);
        }
    };

    public AnnotationsInformationView(@NotNull AbstractInformationControl container, @NotNull SQLEditorBase editor) {
        this.container = container;
        this.editor = editor;
        this.quickAssistAssistant = this.editor.getViewer() != null
            ? this.editor.getViewer().getQuickAssistAssistant()
            : null;
        this.quickFixCommandInfo = obtainQuickFixCommandInfo(editor.getEditorSite());
    }

    public Control createControl(@NotNull Composite parent) {
        this.linksContainer = UIUtils.createComposite(parent, 1);
        return this.linksContainer;
    }

    public void setForceAnnotationIcon(boolean value) {
        this.forceAnnotationIcon = value;
    }

    public void setLinksInformation(@NotNull AnnotationsHoverInfo hoverInfo) {
        this.tooltipAnchorLine = hoverInfo.tooltipAnchorLine;
        this.tooltipAnchorRegion = hoverInfo.hoverRegion;
        if (hoverInfo.annotationsGroups().isEmpty()) {
            this.linksContainer.setLayout(GridLayoutFactory.swtDefaults().spacing(0, 0).margins(0, 0).create());
        } else {
            this.linksContainer.setLayout(GridLayoutFactory.swtDefaults().create());
            for (AnnotationsGroupInfo annotationGroup : hoverInfo.annotationsGroups()) {
                Composite linksGroupContainer;
                if (hoverInfo.annotationsGroups().size() > 1 || forceAnnotationIcon) {
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
                assert !hyperlinks.isEmpty();
                final int alsoLinksLimit = 5;
                final int alsoLinksToShow = Math.min(alsoLinksLimit, hyperlinks.size() - 1) + 1;
                Composite groupLinksContainer = UIUtils.createPlaceholder(
                    linksGroupContainer,
                    hyperlinks.size() == 1 ? 1 : (alsoLinksToShow * 2 + 1),
                    0
                );
                this.createHyperlinkControl(groupLinksContainer, hyperlinks.getFirst(), annotationGroup.getMessage());
                if (hyperlinks.size() > 2) {
                    UIUtils.createLabel(groupLinksContainer, " (also at position ");
                    for (int i = 1; i < alsoLinksToShow; i++) {
                        AnnotationHyperlinkInfo hyperlink = hyperlinks.get(i);
                        if (i > 1) {
                            UIUtils.createLabel(groupLinksContainer, ", ");
                        }
                        this.createHyperlinkControl(groupLinksContainer, hyperlink, Integer.toString(hyperlink.position().getOffset()));
                    }
                    UIUtils.createLabel(
                        groupLinksContainer,
                        hyperlinks.size() <= alsoLinksLimit ? ")" : (", ... " + hyperlinks.size() + " such problems in line)")
                    );
                }
            }
        }
        container.getShell().pack(true);
    }

    public void show() {
        if ((this.tooltipAnchorLine >= 0 || this.tooltipAnchorRegion != null) && editor.getDocument() != null
            && editor.getTextViewer() != null
        ) {
            try {
                IRegion modelLineRange = this.tooltipAnchorRegion != null ? this.tooltipAnchorRegion
                                                                          : editor.getDocument().getLineInformation(this.tooltipAnchorLine);
                IRegion visualLineRange = editor.getTextViewer().modelRange2WidgetRange(modelLineRange);
                StyledText widget = editor.getTextViewer().getTextWidget();
                int offset = visualLineRange.getOffset();
                int length = visualLineRange.getLength();
                Rectangle localLineBounds =  widget.getTextBounds(offset, offset + (length > 0 ? length - 1 : length));
                Rectangle globalLineBounds = Geometry.toDisplay(widget, localLineBounds);
                Rectangle globalWidgetBounds = Geometry.toDisplay(widget, widget.getBounds());

                int y = Math.min(
                    globalLineBounds.y + globalLineBounds.height,
                    globalWidgetBounds.y + globalWidgetBounds.height - widget.getHorizontalBar().getSize().y
                );
                Shell shell = container.getShell();
                boolean hasTooltipRanAway = !globalWidgetBounds.intersects(shell.getBounds());

                Rectangle adjustedBounds = new Rectangle(globalLineBounds.x, y, globalLineBounds.width, globalLineBounds.height);
                if (shell.getBounds().intersects(adjustedBounds) || hasTooltipRanAway || this.tooltipAnchorRegion != null) {
                    int x = hasTooltipRanAway || this.tooltipAnchorRegion != null
                        ? Math.min(widget.getDisplay().getCursorLocation().x, globalLineBounds.x)
                        : shell.getBounds().x;
                    shell.setLocation(new Point(x, y));
                }
                shell.pack(true);
            } catch (BadLocationException e) {
                // nah, no way to adjust position
            }
        }
    }

    private void createHyperlinkControl(
        @NotNull Composite groupLinksContainer,
        @NotNull AnnotationHyperlinkInfo hyperlink,
        @NotNull String text
    ) {
        Composite linkContainer = new Composite(groupLinksContainer, SWT.NONE);
        linkContainer.setLayout(RowLayoutFactory.fillDefaults().spacing(0).create());

        Hyperlink link = new Hyperlink(linkContainer, SWT.NONE);
        link.setHref(hyperlink);
        link.setText(text);
        link.setUnderlined(true);
        link.addHyperlinkListener(this.hyperlinkListener);
        if (hyperlink.annotation() instanceof SQLSemanticErrorAnnotation s) {
            String underlyingError = s.getUnderlyingErrorMessage();
            if (CommonUtils.isNotEmpty(underlyingError)) {
                link.setToolTipText(underlyingError);
            }
        }

        if (this.quickFixCommandInfo != null &&
            this.quickAssistAssistant != null &&
            this.quickAssistAssistant.canFix(hyperlink.annotation)) {
            UIUtils.createLabel(linkContainer, " (");
            Hyperlink fixLink = new Hyperlink(linkContainer, SWT.NONE);
            fixLink.setHref(hyperlink);
            fixLink.setText(this.quickFixCommandInfo.name);
            fixLink.setUnderlined(true);
            fixLink.addHyperlinkListener(this.fixHyperlinkListener);
            fixLink.setToolTipText(this.quickFixCommandInfo.description);
            UIUtils.createLabel(linkContainer, ")");
        }
    }


    public record AnnotationHyperlinkInfo(@NotNull Annotation annotation, @NotNull Position position) {
        public void open(@NotNull SQLEditorBase editor) {
            TextViewer textViewer = editor.getTextViewer();
            if (textViewer != null && !this.position.isDeleted) {
                textViewer.setSelectedRange(this.position.getOffset(), this.position.getLength());
                textViewer.revealRange(this.position.getOffset(), this.position.getLength());
                textViewer.getTextWidget().setFocus();
            }
        }

        public void quickFix(@NotNull SQLEditorBase editor) {
            if (!this.position.isDeleted) {
                editor.selectAndReveal(this.position.getOffset(), 0);
                UIUtils.asyncExec(() -> ActionUtils.runCommand(
                    QUICK_FIX_COMMAND_ID, editor.getSelectionProvider().getSelection(), editor.getSite()
                ));
            }
        }
    }

    public static class AnnotationsGroupInfo {
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

        public AnnotationsGroupInfo(@NotNull String message) {
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
            STMUtils.orderedInsert(this.annotations, e -> e.position().getOffset(), entry, Integer::compare);
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

    public record AnnotationsHoverInfo(
        @NotNull List<AnnotationsGroupInfo> annotationsGroups,
        @Nullable IRegion hoverRegion,
        int tooltipAnchorLine
    ) {
    }

    private record QuickFixCommandInfo(
        @NotNull String name,
        @NotNull String description
    ) {
    }

    /**
     * Collects information for Quick Fix action link in the problem marker tooltip
     */
    @Nullable
    private static QuickFixCommandInfo obtainQuickFixCommandInfo(@NotNull IServiceLocator site) {
        Command command = ActionUtils.findCommand(QUICK_FIX_COMMAND_ID);
        if (command == null) {
            log.error("Failed to resolve command by id '" + QUICK_FIX_COMMAND_ID + "'");
            return null;
        }
        String name;
        String description;
        try {
            name = command.getName();
            description = command.getDescription();
        } catch (Throwable e) {
            log.error("Failed to resolve command parameters for unknown command '" + QUICK_FIX_COMMAND_ID + "'", e);
            return null;
        }

        String shortcut = ActionUtils.findCommandDescription(QUICK_FIX_COMMAND_ID, site, true);
        if (CommonUtils.isNotEmpty(shortcut)) {
            description = CommonUtils.isNotEmpty(description) ? (description + " (" + shortcut + ")") : shortcut;
        }

        return new QuickFixCommandInfo(name, description);
    }
}
