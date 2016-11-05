/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.tools.maintenance;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.ext.exasol.ExasolConstants;
import org.jkiss.dbeaver.ext.exasol.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.ui.UIUtils;

public class ExasolImportTableToolDialog extends ExasolBaseTableToolDialog {

	// Dialog artifacts
	private Combo cbRowSep;
	private Button btInclColNames;
	private Button btSelectDirectory;
	private Button btSelectCompress;
	private Text txColSep;
	private Text txStringSep;
	private Text txFileName;
	private Combo cbEncoding;
	private Label selectedDirectory;
	private String encoding;
	private String rowSep;
	private String filename;

	public ExasolImportTableToolDialog(IWorkbenchPartSite partSite,
			final Collection<ExasolTableBase> selectedTables)
	{
		super(partSite, ExasolMessages.dialog_table_tools_import_title,
				selectedTables);
	}

	@Override
	protected void generateObjectCommand(List<String> sql, ExasolTableBase object)
	{
		StringBuilder sb = new StringBuilder(256);

		// Export String
		sb.append("IMPORT INTO ");
		sb.append(object.getFullyQualifiedName(DBPEvaluationContext.DML));
		sb.append(" FROM LOCAL CSV FILE '");

		// directory was selected
		if (selectedDirectory.getText() != null)
			sb.append(selectedDirectory.getText());

		// name file like table
		sb.append(super.replaceVars(filename, object));
		sb.append(btSelectCompress.getSelection() ? ".csv.gz'" : ".csv'");
		
		// encoding
		sb.append(" ENCODING = '" + encoding + "'");
		
		// row separator
		sb.append(" ROW SEPARATOR = '" + rowSep + "'");
		
		// column separator
		sb.append(" COLUMN SEPARATOR = '" + txColSep.getText().replaceAll("'", "''") + "'");
		
		// string separator
		sb.append(" COLUMN DELIMITER = '" + txStringSep.getText().replaceAll("'", "''") + "'");
		
		
		
		// include column headings
		if (btInclColNames.getSelection())
			sb.append(" SKIP = 1");
		sql.add(sb.toString());

	}

	@Override
	protected void createControls(final Composite parent)
	{
		Group optionsGroup = UIUtils.createControlGroup(parent,
				ExasolMessages.dialog_table_tools_options, 1, 0, 0);
		optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite composite = new Composite(optionsGroup, 2);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Directory select Button
		btSelectDirectory = UIUtils.createPushButton(composite,
				ExasolMessages.dialog_table_open_output_directory, null);
		btSelectDirectory.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				final DirectoryDialog dialog = new DirectoryDialog(parent.getShell());
				final String directory = dialog.open();
				if (directory != null) {
					selectedDirectory.setVisible(true);
					selectedDirectory.setText(directory + File.separatorChar);
				} else {
					selectedDirectory.setVisible(false);
				}
				updateSQL();
			}
		});

		//label for selected directory
		selectedDirectory = UIUtils.createLabel(composite, "");
		selectedDirectory.setVisible(false);

		//file template
		filename = "${schema}_${table}_${date}";
		
		txFileName = UIUtils.createLabelText(composite, ExasolMessages.dialog_table_tools_file_template, filename);
		
		txFileName.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent arg0)
			{
				filename = txFileName.getText();
				updateSQL();
			}
		});
		
		
		
		// compress output
		btSelectCompress = UIUtils.createCheckbox(composite,
				ExasolMessages.dialog_table_tools_export_compress, false);
		btSelectCompress.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				updateSQL();
			}
		});

        // PlaceHolder
		UIUtils.createPlaceholder(composite, 1);
		
		// include column headings
		btInclColNames = UIUtils.createCheckbox(composite, ExasolMessages.dialog_table_tools_column_heading, true);
		btInclColNames.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				updateSQL();
			}
		});
		
        // PlaceHolder
		UIUtils.createPlaceholder(composite, 1);

		// encoding combo
		cbEncoding = UIUtils.createLabelCombo(composite, ExasolMessages.dialog_table_tools_encoding, SWT.DROP_DOWN | SWT.READ_ONLY);
		
		for(String enc: ExasolConstants.encodings)
		{
			cbEncoding.add(enc);
		}
		cbEncoding.select(0);
		encoding = ExasolConstants.encodings.get(0);
		cbEncoding.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				encoding = ExasolConstants.encodings.get(cbEncoding.getSelectionIndex());
				updateSQL();
			}
		});
		
		//  row seperator
		cbRowSep = UIUtils.createLabelCombo(composite, ExasolMessages.dialog_table_tools_string_sep_mode, SWT.DROP_DOWN | SWT.READ_ONLY);
		for (String mode: ExasolConstants.rowSeperators)
		{
			cbRowSep.add(mode);
		}
		cbRowSep.select(0);
		rowSep = ExasolConstants.rowSeperators.get(0);
		cbRowSep.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				rowSep = ExasolConstants.rowSeperators.get(cbRowSep.getSelectionIndex());
				updateSQL();
			}
		});
		
		
		// column sep
		txColSep = UIUtils.createLabelText(composite, ExasolMessages.dialog_table_tools_column_sep, ";");
		txColSep.setTextLimit(1);
		txColSep.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent arg0)
			{
				updateSQL();
			}
		});
		// string sep
		txStringSep = UIUtils.createLabelText(composite, ExasolMessages.dialog_table_tools_string_sep, "\"");
		txStringSep.setTextLimit(1);
		txStringSep.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent arg0)
			{
				updateSQL();
			}
		});

		createObjectsSelector(parent);

	}

}
