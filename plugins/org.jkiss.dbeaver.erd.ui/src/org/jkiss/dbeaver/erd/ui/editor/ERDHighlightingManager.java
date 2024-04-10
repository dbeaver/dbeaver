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

package org.jkiss.dbeaver.erd.ui.editor;

import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.IFigure;
import org.eclipse.swt.graphics.Color;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.model.ERDEntityAttribute;
import org.jkiss.dbeaver.erd.ui.connector.ERDConnection;
import org.jkiss.dbeaver.erd.ui.part.AssociationPart;
import org.jkiss.dbeaver.erd.ui.part.AttributePart;
import org.jkiss.dbeaver.erd.ui.part.EntityPart;
import org.jkiss.dbeaver.utils.ListNode;

import java.util.*;


public class ERDHighlightingManager {

    private static final Log log = Log.getLog(ERDHighlightingManager.class);

    @NotNull
    private final Map<IFigure, PartHighlighter> highlightedParts = new HashMap<>();

    // we are removing entry somewhere in the middle of the highlighting stack, so reference identity is required,
    // which Color class does not follow, as it supports by-value equivalence
    private static final class HighlightingEntry {
        public final Color color;

        public HighlightingEntry(@NotNull Color color) {
            this.color = color;
        }
    }

    private final class PartHighlighter {
        @NotNull
        private final IFigure part;
        @NotNull
        private final Color originalColor;
        private final boolean originalOpaque;
        @NotNull
        private final LinkedList<HighlightingEntry> highlightings = new LinkedList<>();

        public PartHighlighter(@NotNull IFigure part) {
            this.originalColor = part instanceof Connection ? part.getForegroundColor() : part.getBackgroundColor();
            this.originalOpaque = part.isOpaque();
            this.part = part;
        }

        private void refresh() {
            try {
                if (this.highlightings.isEmpty()) {
                    if (part instanceof ERDConnection) {
                        part.setForegroundColor(originalColor);
                    } else {
                        part.setBackgroundColor(originalColor);
                    }
                    part.setOpaque(originalOpaque);
                } else {
                    if (part instanceof ERDConnection) {
                        part.setForegroundColor(highlightings.getLast().color);
                    } else {
                        part.setBackgroundColor(highlightings.getLast().color);
                    }
                    part.setOpaque(true);
                }
                part.repaint();
            } catch (Throwable ex) {
                // any of the figure setters may internally use repaint(), and any of them could use
                // internal object model infrastructure of the Eclipse Graphics Editor, which might be partially invalidated
                // due to the underlying mechanics, which are out of the scope of the highlighting logic
                log.warn("Inconsistent highlighting management detected during figure props refresh.", ex);
            }
        }

        @NotNull
        public ERDHighlightingHandle highlight(@NotNull Color color) {
            HighlightingEntry entry = new HighlightingEntry(color);
            highlightings.addLast(entry);
            this.refresh();
            
            return () -> {
                highlightings.remove(entry);
                if (highlightings.isEmpty()) {
                    highlightedParts.remove(part);
                }
                refresh();
            }; 
        }
        
    }

    @NotNull
    public ERDHighlightingHandle highlight(@NotNull IFigure part, @NotNull Color color) {
        return highlightedParts.computeIfAbsent(part, PartHighlighter::new).highlight(color);
    }

    @Nullable
    public ERDHighlightingHandle makeHighlightingGroupHandle(@Nullable ListNode<ERDHighlightingHandle> highlightings) {
        if (highlightings == null) {
            return null;
        } else {
            return () -> {
                for (ERDHighlightingHandle highlighting: highlightings) {
                    highlighting.release();
                }
            };
        }
    }

    @Nullable
    public ERDHighlightingHandle highlightAttributeAssociations(@NotNull AttributePart attributePart, @NotNull Color color) {
        if (!(attributePart.getParent() instanceof EntityPart entityPart)) {
            return null;
        }
        ListNode<ERDHighlightingHandle> highlightings = null;
        for (AssociationPart associationPart : attributePart.getAssociatingBySource()) {
            if (associationPart.getAssociation().getSourceAttributes().contains(attributePart.getAttribute()) ||
                associationPart.getAssociation().getTargetAttributes().contains(attributePart.getAttribute())) {
                if (associationPart.getConnectionFigure() instanceof ERDConnection erdConnection) {
                    erdConnection.setSelected(!erdConnection.isSelected());
                }
                highlightings = ListNode.join(highlightings, highlightAssociationAndRelatedAttributes(associationPart, color));
            }
        }
        for (AssociationPart associationPart : attributePart.getAssociatingByTarget()) {
            if (associationPart.getAssociation().getSourceAttributes().contains(attributePart.getAttribute()) ||
                associationPart.getAssociation().getTargetAttributes().contains(attributePart.getAttribute())) {
                if (associationPart.getConnectionFigure() instanceof ERDConnection erdConnection) {
                    erdConnection.setSelected(!erdConnection.isSelected());
                }
                highlightings= ListNode.join(highlightings, highlightAssociationAndRelatedAttributes(associationPart, color));
            }
        }
        if (highlightings == null) {
            for (Object connection : entityPart.getSourceConnections()) {
                if (connection instanceof AssociationPart associationPart) {
                    if (associationPart.getAssociation().getSourceAttributes().contains(attributePart.getAttribute()) ||
                        associationPart.getAssociation().getTargetAttributes().contains(attributePart.getAttribute())) {
                        if (associationPart.getConnectionFigure() instanceof ERDConnection erdConnection) {
                            erdConnection.setSelected(!erdConnection.isSelected());
                        }
                        highlightings= ListNode.join(highlightings, highlightAssociationAndRelatedAttributes(associationPart, color));
                    }
                }
            }
        }
        if (highlightings == null) {
            for (Object connection : entityPart.getTargetConnections()) {
                if (connection instanceof AssociationPart associationPart) {
                    if (associationPart.getAssociation().getSourceAttributes().contains(attributePart.getAttribute()) ||
                        associationPart.getAssociation().getTargetAttributes().contains(attributePart.getAttribute())) {
                        if (associationPart.getConnectionFigure() instanceof ERDConnection erdConnection) {
                            erdConnection.setSelected(!erdConnection.isSelected());
                        }
                        highlightings= ListNode.join(highlightings, highlightAssociationAndRelatedAttributes(associationPart, color));
                    }
                }
            }
        }
        return makeHighlightingGroupHandle(highlightings);
    }

    /**
     * The method highlight association and attributes 
     *
     * @param associationPart - AssociationPart
     * @param color - Color
     * @return - ListNode<ERDHighlightingHandle>
     */
    @Nullable
    public ListNode<ERDHighlightingHandle> highlightAssociationAndRelatedAttributes(
        @NotNull AssociationPart associationPart,
        @NotNull Color color
    ) {
        ListNode<ERDHighlightingHandle> highlightings = null;
        EntityPart sourceEntityPart = null;
        if (associationPart.getSource() instanceof EntityPart entityPartFromSource) {
            sourceEntityPart = entityPartFromSource;
        } else if (associationPart.getSource().getParent() instanceof EntityPart entityPartFromParent) {
            sourceEntityPart = entityPartFromParent;
        }
        List<AttributePart> sourcePartAttributes = getEntityAttributes(
            sourceEntityPart,
            associationPart.getAssociation().getSourceAttributes());
        for (AttributePart attrPart : sourcePartAttributes) {
            highlightings = ListNode.push(highlightings, highlight(attrPart.getFigure(), color));
        }
        if (associationPart.getSource() instanceof EntityPart entityPart) {
            for (AttributePart attrPart : getEntityAttributes(entityPart, associationPart.getAssociation().getSourceAttributes())) {
                highlightings = ListNode.push(highlightings, this.highlight(attrPart.getFigure(), color));
            }
        }

        EntityPart targetEntityPart = null;
        if (associationPart.getTarget() instanceof EntityPart entityPartFromSource) {
            targetEntityPart = entityPartFromSource;
        } else if (associationPart.getTarget().getParent() instanceof EntityPart entityPartFromParent) {
            targetEntityPart = entityPartFromParent;
        }
        List<AttributePart> targetPartAttributes = getEntityAttributes(
            targetEntityPart,
            associationPart.getAssociation().getTargetAttributes());
        for (AttributePart attrPart : targetPartAttributes) {
            highlightings = ListNode.push(highlightings, highlight(attrPart.getFigure(), color));
        }

        if (associationPart.getTarget() instanceof EntityPart entityPart) {
            for (AttributePart attrPart : getEntityAttributes(entityPart, associationPart.getAssociation().getTargetAttributes())) {
                highlightings = ListNode.push(highlightings, this.highlight(attrPart.getFigure(), color));
            }
        }
        highlightings = ListNode.push(highlightings, this.highlight(associationPart.getFigure(), color));
        return highlightings;
    }

    @NotNull
    private List<AttributePart> getEntityAttributes(@NotNull EntityPart source, @NotNull List<ERDEntityAttribute> columns) {
        List<AttributePart> result = new ArrayList<>(columns.size());
        for (Object attrPart : source.getChildren()) {
            if (attrPart instanceof AttributePart && columns.contains(((AttributePart) attrPart).getAttribute())) {
                result.add((AttributePart) attrPart);
            }
        }
        return result;
    }
    
}
