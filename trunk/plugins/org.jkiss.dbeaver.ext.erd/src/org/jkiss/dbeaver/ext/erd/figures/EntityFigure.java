/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.figures;

import org.eclipse.draw2d.*;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.DBeaverIcons;

/**
 * Figure used to represent a table in the schema
 *
 * @author Serge Rieder
 */
public class EntityFigure extends Figure {

    private final ERDEntity entity;
    private AttributeListFigure keyFigure;
    private AttributeListFigure attributeFigure;
    private EditableLabel nameLabel;

    public EntityFigure(ERDEntity entity)
    {
        this.entity = entity;

        Image tableImage = DBeaverIcons.getImage(entity.getObject().getEntityType().getIcon());

        keyFigure = new AttributeListFigure(entity, true);
        attributeFigure = new AttributeListFigure(entity, false);
        nameLabel = new EditableLabel(entity.getObject().getName());
        if (tableImage != null) {
            nameLabel.setIcon(tableImage);
        }
        nameLabel.setForegroundColor(ColorConstants.black);

        ToolbarLayout layout = new ToolbarLayout();
        layout.setHorizontal(false);
        layout.setStretchMinorAxis(true);
        setLayoutManager(layout);
        setBorder(new LineBorder(ColorConstants.black, 1));
        setOpaque(true);

        add(nameLabel);
        add(keyFigure);
        add(attributeFigure);

        Label toolTip = new Label(DBUtils.getObjectFullName(entity.getObject()));
        toolTip.setIcon(tableImage);
        setToolTip(toolTip);
        setColors();
    }

    private void setColors() {
        ColorRegistry colorRegistry = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();

        if (entity.isPrimary()) {
            setBackgroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_ENTITY_PRIMARY_BACKGROUND));
        } else if (entity.getObject().getEntityType() == DBSEntityType.ASSOCIATION) {
            setBackgroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_ENTITY_ASSOCIATION_BACKGROUND));
        } else {
            setBackgroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_ENTITY_REGULAR_BACKGROUND));
        }
        setForegroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_ENTITY_NAME_FOREGROUND));
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