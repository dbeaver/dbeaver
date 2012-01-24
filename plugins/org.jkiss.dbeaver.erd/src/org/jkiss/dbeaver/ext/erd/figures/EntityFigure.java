/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
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
            setBackgroundColor(ColorConstants.tooltipBackground);
        }
        setForegroundColor(ColorConstants.tooltipForeground);
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