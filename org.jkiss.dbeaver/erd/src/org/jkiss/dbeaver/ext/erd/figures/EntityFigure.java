/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.figures;

import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.swt.graphics.Color;

/**
 * Figure used to represent a table in the schema
 * @author Phil Zoio
 */
public class EntityFigure extends Figure
{

	public static Color tableColor = new Color(null, 255, 255, 206);
	private AttributeFigure attributeFigure = new AttributeFigure();
	private EditableLabel nameLabel;

	public EntityFigure(EditableLabel name)
	{
		this(name, null);
	}

	public EntityFigure(EditableLabel name, List colums)
	{

		nameLabel = name;
		ToolbarLayout layout = new ToolbarLayout();
		layout.setVertical(true);
		layout.setStretchMinorAxis(true);
		setLayoutManager(layout);
		setBorder(new LineBorder(ColorConstants.black, 1));
		setBackgroundColor(tableColor);
		setForegroundColor(ColorConstants.black);
		setOpaque(true);

		name.setForegroundColor(ColorConstants.black);
		add(name);
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