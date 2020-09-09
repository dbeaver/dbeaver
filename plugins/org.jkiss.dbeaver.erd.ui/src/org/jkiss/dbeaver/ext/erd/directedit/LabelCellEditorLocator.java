/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.erd.directedit;

import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.tools.CellEditorLocator;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Text;

/**
 * A CellEditorLocator for a specified label
 * 
 * @author Serge Rider
 */
public class LabelCellEditorLocator implements CellEditorLocator
{

	private Label label;

	/**
	 * Creates a new CellEditorLocator for the given Label
	 * 
	 * @param label
	 *            the Label
	 */
	public LabelCellEditorLocator(Label label)
	{
		setLabel(label);
	}

	/**
	 * expands the size of the control by 1 pixel in each direction
	 */
	@Override
    public void relocate(CellEditor celleditor)
	{
		Text text = (Text) celleditor.getControl();

		Point pref = text.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		Rectangle rect = label.getTextBounds().getCopy();
		label.translateToAbsolute(rect);
		if (text.getCharCount() > 1)
			text.setBounds(rect.x - 1, rect.y - 1, pref.x + 1, pref.y + 1);
		else
			text.setBounds(rect.x - 1, rect.y - 1, pref.y + 1, pref.y + 1);

	}

	/**
	 * Returns the Label figure.
	 * 
	 * @return the Label
	 */
	protected Label getLabel()
	{
		return label;
	}

	/**
	 * Sets the label.
	 * 
	 * @param label
	 *            The label to set
	 */
	protected void setLabel(Label label)
	{
		this.label = label;
	}

}