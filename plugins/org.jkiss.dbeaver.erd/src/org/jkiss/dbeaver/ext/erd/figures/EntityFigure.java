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
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * Figure used to represent a table in the schema
 *
 * @author Serge Rieder
 */
public class EntityFigure extends Figure {

    public static Color primaryTableColor = new Color(null, 255, 226, 255);

    private AttributeFigure attributeFigure;
    private EditableLabel nameLabel;

    public EntityFigure(ERDEntity entity)
    {
        DBNDatabaseNode entityNode = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(entity.getObject());
        Image tableImage;
        if (entityNode == null) {
            tableImage = DBIcon.TREE_TABLE.getImage();
        } else {
            tableImage = entityNode.getNodeIconDefault();
        }

        attributeFigure = new AttributeFigure(entity);
        nameLabel = new EditableLabel(entity.getObject().getName());
        nameLabel.setIcon(tableImage);
        nameLabel.setForegroundColor(ColorConstants.black);

        ToolbarLayout layout = new ToolbarLayout();
        layout.setVertical(true);
        layout.setStretchMinorAxis(true);
        setLayoutManager(layout);
        setBorder(new LineBorder(ColorConstants.black, 1));
        if (entity.isPrimary()) {
            setBackgroundColor(primaryTableColor);
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