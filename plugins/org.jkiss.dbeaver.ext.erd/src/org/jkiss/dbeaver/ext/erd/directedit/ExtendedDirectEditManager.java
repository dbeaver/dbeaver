/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.erd.directedit;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.tools.CellEditorLocator;
import org.eclipse.gef.tools.DirectEditManager;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Text;

/**
 * A generic DirectEdit manager to be used for labels which includes validation
 * functionality by adding the ICellEditorValidator on startup
 */
public class ExtendedDirectEditManager extends DirectEditManager
{

	Font figureFont;
	protected VerifyListener verifyListener;
	protected Label label;
	protected String originalValue;
	private boolean committing = false;
	private ICellEditorValidator validator = null;

	/**
	 * Creates a new ActivityDirectEditManager with the given attributes.
	 * 
	 * @param source
	 *            the source EditPart
	 * @param editorType
	 *            type of editor
	 * @param locator
	 *            the CellEditorLocator
	 */
	public ExtendedDirectEditManager(GraphicalEditPart source, Class editorType, CellEditorLocator locator,
			Label label, ICellEditorValidator validator)
	{
		super(source, editorType, locator);
		this.label = label;
		this.originalValue = label.getText();
		this.validator = validator;
	}

	/**
	 * @see org.eclipse.gef.tools.DirectEditManager#bringDown()
	 */
	@Override
    protected void bringDown()
	{
		Font disposeFont = figureFont;
		figureFont = null;
		super.bringDown();
		if (disposeFont != null)
			disposeFont.dispose();
	}

	/**
	 * @see org.eclipse.gef.tools.DirectEditManager#initCellEditor()
	 */
	@Override
    protected void initCellEditor()
	{

		Text text = (Text) getCellEditor().getControl();

		//add the verifyListener to apply changes to the control size
		verifyListener = new VerifyListener()
		{

			/**
			 * Changes the size of the editor control to reflect the changed
			 * text
			 */
			@Override
            public void verifyText(VerifyEvent event)
			{
				Text text = (Text) getCellEditor().getControl();
				String oldText = text.getText();
				String leftText = oldText.substring(0, event.start);
				String rightText = oldText.substring(event.end, oldText.length());
				GC gc = new GC(text);
				if (leftText == null)
					leftText = "";
				if (rightText == null)
					rightText = "";

				String s = leftText + event.text + rightText;

				Point size = gc.textExtent(leftText + event.text + rightText);

				gc.dispose();
				if (size.x != 0)
					size = text.computeSize(size.x, SWT.DEFAULT);
				else
				{
					//just make it square
					size.x = size.y;
				}
				getCellEditor().getControl().setSize(size.x, size.y);
			}

		};
		text.addVerifyListener(verifyListener);

		//set the initial value of the
		originalValue = this.label.getText();
		getCellEditor().setValue(originalValue);

		//calculate the font size of the underlying
		IFigure figure = getEditPart().getFigure();
		figureFont = figure.getFont();
		FontData data = figureFont.getFontData()[0];
		Dimension fontSize = new Dimension(0, data.getHeight());

		//set the font to be used
		this.label.translateToAbsolute(fontSize);
		data.setHeight(fontSize.height);
		figureFont = new Font(null, data);

		//set the validator for the CellEditor
		getCellEditor().setValidator(validator);

		text.setFont(figureFont);
		text.selectAll();
	}

	/**
	 * Commits the current value of the cell editor by getting a {@link Command}
	 * from the source edit part and executing it via the {@link CommandStack}.
	 * Finally, {@link #bringDown()}is called to perform and necessary cleanup.
	 */
	@Override
    protected void commit()
	{

		if (committing)
			return;
		committing = true;
		try
		{

			//we set the cell editor control to invisible to remove any
			// possible flicker
			getCellEditor().getControl().setVisible(false);
			if (isDirty())
			{
				CommandStack stack = getEditPart().getViewer().getEditDomain().getCommandStack();
				Command command = getEditPart().getCommand(getDirectEditRequest());

				if (command != null && command.canExecute())
					stack.execute(command);
			}
		}
		finally
		{
			bringDown();
			committing = false;
		}
	}

	/**
	 * Need to override so as to remove the verify listener
	 */
	@Override
    protected void unhookListeners()
	{
		super.unhookListeners();
		Text text = (Text) getCellEditor().getControl();
		text.removeVerifyListener(verifyListener);
		verifyListener = null;
	}

}