/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 27, 2004
 */
package org.jkiss.dbeaver.ext.erd.editor.wizard;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * Wizard to create new schema diagram
 * @author Phil Zoio
 */
public class SchemaDiagramWizard extends Wizard implements INewWizard
{

	private SchemaDiagramWizardPage schemaPage = null;
	private IStructuredSelection selection;
	private IWorkbench workbench;

	public void addPages()
	{
		schemaPage = new SchemaDiagramWizardPage(workbench, selection);
		addPage(schemaPage);
	}

	public void init(IWorkbench aWorkbench, IStructuredSelection currentSelection)
	{
		workbench = aWorkbench;
		selection = currentSelection;
	}

	public boolean performFinish()
	{
		return schemaPage.finish();
	}

}