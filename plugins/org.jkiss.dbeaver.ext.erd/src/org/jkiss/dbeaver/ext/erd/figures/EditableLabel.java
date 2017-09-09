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

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * A customized Label based on the label used in the flow example. 
 * Primary selection is denoted by highlight and focus rectangle. 
 * Normal selection is denoted by highlight only. Borrowed from the Flow Editor example
 * @author Daniel Lee
 */
public class EditableLabel extends Label
{

	private boolean selected;
	
	public EditableLabel(String text)
	{
		super(text);
	}
	
	private Rectangle getSelectionRectangle()
	{
		Rectangle bounds = getTextBounds().getCopy();
		bounds.expand(new Insets(2, 2, 0, 0));
		translateToParent(bounds);
		bounds.intersect(getBounds());
		return bounds;
	}

	
	/**
	 * sets the text of the label
	 */
	@Override
    public void setText(String s)
	{
		super.setText(s);
	}
	
	
	/**
	 * paints figure differently depends on the whether the figure has focus or is selected 
	 */
	@Override
    protected void paintFigure(Graphics graphics)
	{
		if (selected)
		{
			graphics.pushState();
			graphics.setBackgroundColor(UIUtils.getColorRegistry().get(ERDConstants.COLOR_ERD_ATTR_FOREGROUND));
			graphics.fillRoundRectangle(getSelectionRectangle(), 3, 3);
			graphics.popState();
			graphics.setForegroundColor(UIUtils.getColorRegistry().get(ERDConstants.COLOR_ERD_DIAGRAM_BACKGROUND));
		}
		super.paintFigure(graphics);
	}

	/**
	 * Sets the selection state of this SimpleActivityLabel
	 * 
	 * @param b
	 *            true will cause the label to appear selected
	 */
	public void setSelected(boolean b)
	{
		selected = b;
		repaint();
	}


}

