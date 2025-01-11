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
package org.jkiss.dbeaver.erd.ui.figures;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.erd.model.ERDEntityAttribute;
import org.jkiss.dbeaver.erd.ui.editor.ERDThemeSettings;
import org.jkiss.dbeaver.erd.ui.editor.ERDViewStyle;
import org.jkiss.dbeaver.erd.ui.model.ERDDecorator;
import org.jkiss.dbeaver.erd.ui.model.EntityDiagram;
import org.jkiss.dbeaver.erd.ui.part.AttributePart;
import org.jkiss.dbeaver.erd.ui.part.DiagramPart;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBeaverIcons;

import java.util.List;

/**
 * Figure used to hold the column labels
 * @author Serge Rider
 */
public class AttributeItemFigure extends Figure
{
    protected final AttributePart part;
    private IFigure rightPanel;

    public AttributeItemFigure(AttributePart part)
	{
        super();
        this.part = part;

        ERDEntityAttribute attribute = part.getAttribute();
        GridLayout layout = new GridLayout(3, false);
        layout.marginHeight = 2;
        layout.marginWidth = 5;
        layout.verticalSpacing = 0;
        layout.horizontalSpacing = 3;
        setLayoutManager(layout);

        EntityDiagram diagram = part.getDiagramPart().getDiagram();

        boolean showCheckboxes = diagram.getDecorator().showCheckboxes();
        if (showCheckboxes) {
            CustomCheckBoxFigure attrCheckbox = new CustomCheckBoxFigure();
            attrCheckbox.setSelected(attribute.isChecked());
            attrCheckbox.addChangeListener(changeEvent -> {
                boolean oldChecked = attribute.isChecked();
                boolean newChecked = attrCheckbox.isSelected();
                if (oldChecked != newChecked) {
                    part.getDiagramPart().getViewer().getEditDomain().getCommandStack().execute(
                            part.createAttributeCheckCommand(newChecked)
                    );
                }
            });
            add(attrCheckbox);
        }

        EditableLabel attrNameLabel = new EditableLabel(part.getAttributeLabel());

        if (diagram.hasAttributeStyle(ERDViewStyle.ICONS)) {
            DBPImage labelImage = attribute.getLabelImage();
            if (labelImage != null) {
                attrNameLabel.setIcon(DBeaverIcons.getImage(labelImage));
            }
        }
        add(attrNameLabel);

        DiagramPart diagramPart = part.getDiagramPart();
        Font columnFont = diagramPart.getNormalFont();
        Color columnColor = getColumnForegroundColor();
        if (part.getAttribute().isInPrimaryKey()) {
            columnFont = diagramPart.getBoldFont();
        }

        attrNameLabel.setFont(columnFont);
        attrNameLabel.setForegroundColor(columnColor);
        if (rightPanel != null) {
            rightPanel.setFont(columnFont);
            rightPanel.setForegroundColor(columnColor);
        }
	}

    protected Color getColumnForegroundColor() {
        return ERDThemeSettings.instance.attrForeground;
        //return part.getDiagramPart().getContentPane().getForegroundColor();
    }

    public ERDEntityAttribute getAttribute() {
        return part.getAttribute();
    }

    @Nullable
    public CustomCheckBoxFigure getCheckBox() {
	    if (getChildren().size() < 2) {
	        return null;
        }
        return (CustomCheckBoxFigure) getChildren().get(0);
    }

    @NotNull
    public EditableLabel getLabel() {
        List children = getChildren();
        return (EditableLabel) children.get(children.size() == 1 ? 0 : 1);
    }

    public IFigure getRightPanel() {
        return rightPanel;
    }

    void setRightPanel(IFigure attrExtra) {
        this.rightPanel = attrExtra;
    }

    public void updateLabels() {
        getLabel().setText(part.getAttributeLabel());

        final EntityDiagram diagram = part.getDiagram();
        final ERDDecorator decorator = diagram.getDecorator();

        if (decorator.supportsAttributeStyle(ERDViewStyle.ICONS) && diagram.hasAttributeStyle(ERDViewStyle.ICONS)) {
            DBPImage labelImage = part.getAttribute().getLabelImage();
            if (labelImage != null) {
                getLabel().setIcon(DBeaverIcons.getImage(labelImage));
            }
        }

        if (rightPanel instanceof Label) {

            String rightText = "";
            if (decorator.supportsAttributeStyle(ERDViewStyle.TYPES) && diagram.hasAttributeStyle(ERDViewStyle.TYPES)) {
                rightText = part.getAttribute().getObject().getFullTypeName();
            }
            if (decorator.supportsAttributeStyle(ERDViewStyle.NULLABILITY) && diagram.hasAttributeStyle(ERDViewStyle.NULLABILITY)) {
                if (part.getAttribute().getObject().isRequired()) {
                    rightText += " NOT NULL";
                }
            }
            ((Label)rightPanel).setText(rightText);
        }
    }

    @Override
    public Rectangle getBounds() {
        final IFigure parent = getParent();
        if (parent != null && parent.getBorder() != null) {
            // Extend bounds to the parent's width. This is required for navigation to work correctly:
            // If there's two attributes whose names have different length (e.g. 'id' and 'description'),
            // descending direction between them would be 'east', not 'south', since that's what .gef thinks.
            // See org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler.getNavigationPoint
            final Insets insets = parent.getBorder().getInsets(this);
            final Rectangle bounds = parent.getBounds();
            return super.getBounds().getCopy()
                .setX(bounds.x + insets.left)
                .setWidth(bounds.width - insets.left - insets.right);
        }
        return super.getBounds();
    }
}
