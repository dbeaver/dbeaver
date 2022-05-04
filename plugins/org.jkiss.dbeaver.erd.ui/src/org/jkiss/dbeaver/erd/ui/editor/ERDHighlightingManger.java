/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import java.util.*;

import org.eclipse.draw2dl.Connection;
import org.eclipse.draw2dl.IFigure;
import org.eclipse.swt.graphics.Color;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.erd.model.ERDEntityAttribute;
import org.jkiss.dbeaver.erd.ui.part.AssociationPart;
import org.jkiss.dbeaver.erd.ui.part.AttributePart;
import org.jkiss.dbeaver.erd.ui.part.EntityPart;
import org.jkiss.dbeaver.model.sql.parser.ListNode;


public class ERDHighlightingManger {

    @NotNull
    private final Map<IFigure, PartHighlighter> highlightedParts = new HashMap<>();

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
                    if (part instanceof Connection) {
                        part.setForegroundColor(originalColor);
                    } else {
                        part.setBackgroundColor(originalColor);
                    }
                    part.setOpaque(originalOpaque);
                } else {
                    if (part instanceof Connection) {
                        part.setForegroundColor(highlightings.getLast().color);
                    } else {
                        part.setBackgroundColor(highlightings.getLast().color);
                    }
                    part.setOpaque(true);
                }
                part.repaint();
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
        
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
        return highlightedParts.computeIfAbsent(part, p -> new PartHighlighter(p)).highlight(color);
    }
    
    private ERDHighlightingHandle makeHighlightingGroupHandle(ListNode<ERDHighlightingHandle> highlightings) {
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
    
    public ERDHighlightingHandle highlightAttributeAssociations(AttributePart attributePart, Color color) {
        if (!(attributePart.getParent() instanceof EntityPart)) {
            return null; 
        }
        
        ListNode<ERDHighlightingHandle> highlightings = null; 
        EntityPart entityPart = (EntityPart)attributePart.getParent();
        
        for (Object connection : entityPart.getSourceConnections()) {
            if (connection instanceof AssociationPart) {
                highlightings = this.highlightAttributeAssociation(attributePart, (AssociationPart) connection, color, highlightings);
            }
        }
        for (Object connection : entityPart.getTargetConnections()) {
            if (connection instanceof AssociationPart) {
                highlightings = this.highlightAttributeAssociation(attributePart, (AssociationPart) connection, color, highlightings);                
            }
        }
        
        return this.makeHighlightingGroupHandle(highlightings);
    }
    
    private ListNode<ERDHighlightingHandle> highlightAttributeAssociation(AttributePart attributePart, AssociationPart associationPart, Color color, ListNode<ERDHighlightingHandle> highlightings) {
        if (associationPart.getAssociation().getSourceAttributes().contains(attributePart.getAttribute()) ||
            associationPart.getAssociation().getTargetAttributes().contains(attributePart.getAttribute())) {
            highlightings = ListNode.push(highlightings, this.highlight(associationPart.getFigure(), color));
            return highlightAssociationRelatedAttributes(associationPart, color, highlightings);
        } else {
            return highlightings;
        }
    }

    public ERDHighlightingHandle highlightAssociationAndRelatedAttributes(AssociationPart associationPart, Color color) {
        return this.makeHighlightingGroupHandle(highlightAssociationRelatedAttributes(associationPart, color, null));
    }
    
    private ListNode<ERDHighlightingHandle> highlightAssociationRelatedAttributes(AssociationPart associationPart, Color color, ListNode<ERDHighlightingHandle> highlightings) {

        if (associationPart.getSource() instanceof EntityPart) {
            for (AttributePart attrPart : getEntityAttributes((EntityPart) associationPart.getSource(), associationPart.getAssociation().getSourceAttributes())) {
                highlightings = ListNode.push(highlightings, this.highlight(attrPart.getFigure(), color));
            }
        }
        if (associationPart.getTarget() instanceof EntityPart) {
            for (AttributePart attrPart : getEntityAttributes((EntityPart) associationPart.getTarget(), associationPart.getAssociation().getTargetAttributes())) {
                highlightings = ListNode.push(highlightings, this.highlight(attrPart.getFigure(), color));
            }
        }
        
        return highlightings;
    }

    private List<AttributePart> getEntityAttributes(EntityPart source, List<ERDEntityAttribute> columns) {
        List<AttributePart> result = new ArrayList<>(columns.size());
        for (Object attrPart : source.getChildren()) {
            if (attrPart instanceof AttributePart && columns.contains(((AttributePart)attrPart).getAttribute())) {
                result.add((AttributePart)attrPart);
            }
        }
        return result;
    }
    
}
