/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 27, 2004
 */
package org.jkiss.dbeaver.ext.erd.editor.wizard;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.ide.IDE;

import org.jkiss.dbeaver.ext.erd.model.Schema;

/**
 * Page implementated to create new schema editor diagram
 * @author Phil Zoio
 */
public class SchemaDiagramWizardPage extends WizardNewFileCreationPage implements SelectionListener
{

	private IWorkbench workbench;
	private Button emptyDiagramButton = null;
	private Button preCreatedButtonDiagram = null;
	private boolean emptyModel = true;

	public SchemaDiagramWizardPage(IWorkbench aWorkbench, IStructuredSelection selection)
	{
		super("schemaWizardPage", selection);
		this.setTitle("Create Database Schema Diagram");
		this.setDescription("Create the schema database diagram GEF demo");
		this.workbench = aWorkbench;
	}

	public void createControl(Composite parent)
	{
		super.createControl(parent);
		setFileName("change_me.schema");

		Composite composite = (Composite) getControl();

		// initial model selection group
		Group group = new Group(composite, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setText("Select Schema Model");
		group.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		// initial model selection checkboxes
		emptyDiagramButton = new Button(group, SWT.RADIO);
		emptyDiagramButton.setText("Empty Schema Model");
		emptyDiagramButton.addSelectionListener(this);
		emptyDiagramButton.setSelection(true);

		preCreatedButtonDiagram = new Button(group, SWT.RADIO);
		preCreatedButtonDiagram.setText("Pre-created Sample Schema Model");
		preCreatedButtonDiagram.addSelectionListener(this);

		new Label(composite, SWT.NONE);

		setPageComplete(validatePage());
	}

	protected InputStream getInitialContents()
	{
		Schema ld = new Schema(getFileName());
		ByteArrayInputStream bais = null;
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(ld);
			oos.flush();
			oos.close();
			baos.close();
			bais = new ByteArrayInputStream(baos.toByteArray());
			bais.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return bais;
	}

	protected boolean validatePage()
	{
		boolean result = super.validatePage();
		if (result)
		{
			String fileName = getFileName();
			if (!fileName.endsWith(".schema"))
			{
				result = false;
				setErrorMessage("File name must end in .schema");
			}
		}
		return result;
	}

	public boolean finish()
	{
		IFile newFile = createNewFile();
		if (newFile == null)
			return false;

		// Since the file resource was created fine, open it for editing
		// iff requested by the user
		try
		{
			IWorkbenchWindow dwindow = workbench.getActiveWorkbenchWindow();
			IWorkbenchPage page = dwindow.getActivePage();
			if (page != null)
				IDE.openEditor(page, newFile, true);
		}
		catch (PartInitException e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Modify the flag indicating whether to use the pre-populated or empty schema based on the user selection 
	 */
	public void widgetSelected(SelectionEvent e)
	{
		if (e.getSource() == emptyDiagramButton)
		{
			emptyModel = true;
		}
		else if (e.getSource() == preCreatedButtonDiagram)
		{
			emptyModel = false;
		}

	}

	/**
	 * No implementation
	 */
	public void widgetDefaultSelected(SelectionEvent e)
	{
	}

}