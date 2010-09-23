/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.figures;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.swt.graphics.Color;
import org.jkiss.dbeaver.ext.erd.model.ERDTable;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * Figure used to represent a table in the schema
 * @author Serge Rieder
 */
public class EntityFigure extends Figure
{

    public static Color primaryTableColor = new Color(null, 255, 226, 255);

    private ERDTable table;
	private AttributeFigure attributeFigure;
	private EditableLabel nameLabel;

	public EntityFigure(ERDTable table)
	{
        this.table = table;
        attributeFigure = new AttributeFigure(table);
        nameLabel = new EditableLabel(table.getObject().getName());
        nameLabel.setIcon(table.getObject().isView() ? DBIcon.TREE_VIEW.getImage() : DBIcon.TREE_TABLE.getImage());
        nameLabel.setForegroundColor(ColorConstants.black);

		ToolbarLayout layout = new ToolbarLayout();
		layout.setVertical(true);
		layout.setStretchMinorAxis(true);
		setLayoutManager(layout);
		setBorder(new LineBorder(ColorConstants.black, 1));
        if (table.isPrimary()) {
            setBackgroundColor(primaryTableColor);
        } else {
		    setBackgroundColor(ColorConstants.tooltipBackground);
        }
		setForegroundColor(ColorConstants.black);
		setOpaque(true);

		add(nameLabel);
		add(attributeFigure);

	}

	public void setSelected(boolean isSelected)
	{
		LineBorder lineBorder = (LineBorder) getBorder();
		if (isSelected)
		{
			lineBorder.setWidth(2);
		}
		else
		{
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