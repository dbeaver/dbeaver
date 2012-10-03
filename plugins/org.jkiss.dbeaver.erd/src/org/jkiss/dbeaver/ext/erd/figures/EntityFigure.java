/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.figures;

import org.eclipse.draw2d.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityType;

/**
 * Figure used to represent a table in the schema
 *
 * @author Serge Rieder
 */
public class EntityFigure extends Figure {

    public static Color primaryTableColor = new Color(null, 255, 226, 255);
    public static Color associationTableColor = new Color(null, 255, 255, 255);
    public static Color commonTableColor = new Color(null, 0xff, 0xff, 0xe1);
    public static Color tableNameColor = new Color(null, 0, 0, 0);

    private AttributeFigure attributeFigure;
    private EditableLabel nameLabel;

    public EntityFigure(ERDEntity entity)
    {
        Image tableImage = entity.getObject().getEntityType().getIcon();

        attributeFigure = new AttributeFigure(entity);
        nameLabel = new EditableLabel(entity.getObject().getName());
        if (tableImage != null) {
            nameLabel.setIcon(tableImage);
        }
        nameLabel.setForegroundColor(ColorConstants.black);

        ToolbarLayout layout = new ToolbarLayout();
        layout.setVertical(true);
        layout.setStretchMinorAxis(true);
        setLayoutManager(layout);
        setBorder(new LineBorder(ColorConstants.black, 1));
        if (entity.isPrimary()) {
            setBackgroundColor(primaryTableColor);
        } else if (entity.getObject().getEntityType() == DBSEntityType.ASSOCIATION) {
            setBackgroundColor(associationTableColor);
        } else {
            setBackgroundColor(commonTableColor);
        }
        setForegroundColor(tableNameColor);
        setOpaque(true);

        add(nameLabel);
        add(attributeFigure);

        Label toolTip = new Label(DBUtils.getObjectFullName(entity.getObject()));
        toolTip.setIcon(tableImage);
        setToolTip(toolTip);
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

    /**
     * @return the figure containing the column lables
     */
    public AttributeFigure getColumnsFigure()
    {
        return attributeFigure;
    }

}