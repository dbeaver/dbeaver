/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.erd.action;

import org.eclipse.jface.action.Action;
import org.jkiss.dbeaver.ext.erd.ERDActivator;
import org.jkiss.dbeaver.ext.erd.ERDMessages;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorPart;

/**
 * Action to toggle the layout between manual and automatic
 *
 * @author Serge Rider
 */
public class DiagramLayoutAction extends Action
{
	private ERDEditorPart editor;

	public DiagramLayoutAction(ERDEditorPart editor)
	{
		super(ERDMessages.action_diagram_layout_name, ERDActivator.getImageDescriptor("icons/arrangeall.png")); //$NON-NLS-2$
		this.editor = editor;
	}

	@Override
    public void run()
	{
        //editor.get
        editor.getDiagramPart().rearrangeDiagram();
	}

}