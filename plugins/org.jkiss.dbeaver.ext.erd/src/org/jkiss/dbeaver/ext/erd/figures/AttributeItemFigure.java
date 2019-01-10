/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.erd.figures;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.erd.editor.ERDViewStyle;
import org.jkiss.dbeaver.ext.erd.model.ERDEntityAttribute;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.part.AttributePart;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
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

        ToolbarLayout layout = new ToolbarLayout(true);

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
        Color columnColor = diagramPart.getContentPane().getForegroundColor();
        if (part.getAttribute().isInPrimaryKey()) {
            columnFont = diagramPart.getBoldFont();
        }

        setFont(columnFont);
        setForegroundColor(columnColor);
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
        if (rightPanel instanceof Label) {

            String rightText = "";
            if (part.getDiagram().hasAttributeStyle(ERDViewStyle.TYPES)) {
                rightText = part.getAttribute().getObject().getFullTypeName();
            }
            if (part.getDiagram().hasAttributeStyle(ERDViewStyle.NULLABILITY)) {
                if (part.getAttribute().getObject().isRequired()) {
                    rightText += " NOT NULL";
                }
            }
            ((Label)rightPanel).setText(rightText);
        }
    }
}
