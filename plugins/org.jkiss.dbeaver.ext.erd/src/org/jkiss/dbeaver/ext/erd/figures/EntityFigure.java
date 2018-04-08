/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.figures;

import org.eclipse.draw2d.*;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Figure used to represent a table in the schema
 *
 * @author Serge Rider
 */
public class EntityFigure extends Figure {

    private final ERDEntity entity;
    private AttributeListFigure keyFigure;
    private AttributeListFigure attributeFigure;
    private EditableLabel nameLabel;

    public EntityFigure(ERDEntity entity, boolean useFQN)
    {
        this.entity = entity;

        Image tableImage = DBeaverIcons.getImage(entity.getObject().getEntityType().getIcon());

        keyFigure = new AttributeListFigure(entity, true);
        attributeFigure = new AttributeListFigure(entity, false);
        nameLabel = new EditableLabel(
            useFQN ?
                DBUtils.getObjectFullName(entity.getObject(), DBPEvaluationContext.DDL) :
                entity.getObject().getName());
        if (tableImage != null) {
            nameLabel.setIcon(tableImage);
        }

        ToolbarLayout layout = new ToolbarLayout();
        layout.setHorizontal(false);
        layout.setStretchMinorAxis(true);
        setLayoutManager(layout);
        setBorder(new LineBorder(UIUtils.getColorRegistry().get(ERDConstants.COLOR_ERD_ENTITY_NAME_FOREGROUND), 1));
        setOpaque(true);

        add(nameLabel);
        add(keyFigure);
        add(attributeFigure);

        Label toolTip = new Label(DBUtils.getObjectFullName(entity.getObject(), DBPEvaluationContext.UI));
        toolTip.setIcon(tableImage);
        setToolTip(toolTip);
        setColors();
    }

    private void setColors() {
        ColorRegistry colorRegistry = UIUtils.getColorRegistry();

        if (entity.isPrimary()) {
            setBackgroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_ENTITY_PRIMARY_BACKGROUND));
        } else if (entity.getObject().getEntityType() == DBSEntityType.ASSOCIATION) {
            setBackgroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_ENTITY_ASSOCIATION_BACKGROUND));
        } else {
            setBackgroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_ENTITY_REGULAR_BACKGROUND));
        }
        setForegroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_ENTITY_NAME_FOREGROUND));
        nameLabel.setForegroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_ENTITY_NAME_FOREGROUND));
    }

    public void setSelected(boolean isSelected)
    {
        LineBorder lineBorder = (LineBorder) getBorder();
        if (isSelected) {
            lineBorder.setWidth(2);
        } else {
            lineBorder.setWidth(1);
        }
    }


    /**
     * @return returns the label used to edit the name
     */
    public EditableLabel getNameLabel()
    {
        return nameLabel;
    }

    public AttributeListFigure getKeyFigure() {
        return keyFigure;
    }

    /**
     * @return the figure containing the column lables
     */
    public AttributeListFigure getColumnsFigure()
    {
        return attributeFigure;
    }

    @Override
    public void add(IFigure figure, Object constraint, int index) {
        if (figure instanceof AttributeItemFigure) {
            ColorRegistry colorRegistry = UIUtils.getColorRegistry();
            figure.setForegroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_ATTR_FOREGROUND));
            figure.setBackgroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_ATTR_BACKGROUND));

            if (((AttributeItemFigure) figure).getAttribute().isInPrimaryKey()) {
                keyFigure.add(figure, constraint, -1);
            } else {
                attributeFigure.add(figure, constraint, -1);
            }

        } else {
            super.add(figure, constraint, index);
        }
    }
}