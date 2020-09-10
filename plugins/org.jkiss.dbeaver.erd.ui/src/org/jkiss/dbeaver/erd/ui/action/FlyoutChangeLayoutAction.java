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
 * Created on Jul 23, 2004
 */
package org.jkiss.dbeaver.erd.ui.action;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.erd.ui.editor.ERDEditorPart;
import org.jkiss.dbeaver.erd.ui.model.EntityDiagram;

/**
 * Action to toggle the layout between manual and automatic
 * 
 * @author Serge Rider
 */
public class FlyoutChangeLayoutAction extends Action
{

	private IEditorPart editor;
	private boolean checked;

	public FlyoutChangeLayoutAction(IEditorPart editor)
	{
		super("Automatic Layout", Action.AS_CHECK_BOX);
		this.editor = editor;
	}

	@Override
    public void run()
	{
		if (editor instanceof ERDEditorPart)
		{
			ERDEditorPart erdEditor = (ERDEditorPart) editor;
			EntityDiagram entityDiagram = erdEditor.getDiagram();
			boolean isManual = entityDiagram.isLayoutManualDesired();
			entityDiagram.setLayoutManualDesired(!isManual);
            erdEditor.getDiagramPart().changeLayout();

			checked = !isManual;
			setChecked(checked);
		}
	}

	@Override
    public boolean isChecked()
	{
		if (editor != null)
			return isChecked(editor);
		else
			return super.isChecked();
	}

	/**
	 * @see org.eclipse.jface.action.IAction#isChecked()
	 */
	public boolean isChecked(IEditorPart editor)
	{

		if (editor instanceof ERDEditorPart)
		{
			ERDEditorPart schemaEditor = (ERDEditorPart) editor;
			EntityDiagram entityDiagram = schemaEditor.getDiagram();
			boolean checkTrue = entityDiagram.isLayoutManualDesired();
			return (!checkTrue);
		}
		else
		{
			return false;
		}

	}

	public void setActiveEditor(IEditorPart editor)
	{
		this.editor = editor;
		boolean localChecked = isChecked(editor);
		
		//there appears to be a bug in the framework which necessitates this
		if (localChecked)
			firePropertyChange(CHECKED, Boolean.FALSE, Boolean.TRUE);
		else
			firePropertyChange(CHECKED, Boolean.TRUE, Boolean.FALSE);
	}

}