package org.jkiss.dbeaver.ext.dm.ui.editors;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.ext.dm.model.DmDDLFormat;
import org.jkiss.dbeaver.ext.dm.model.DmTable;
import org.jkiss.dbeaver.ext.dm.model.utils.DmConstants;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;

/**
 * DM Object DLL Editor
 * 
 * @author caosw
 *
 */
public class DmObjectDDLEditor extends SQLSourceViewer<DmTable> {

	public DmObjectDDLEditor() {
	}

	@Override
	protected void contributeEditorCommands(IContributionManager contributionManager) {
		super.contributeEditorCommands(contributionManager);
		contributionManager.add(new Separator());
		contributionManager.add(new ControlContribution("DDLFormat") {

			@Override
			protected Control createControl(Composite parent) {
				DmDDLFormat ddlFormat = DmDDLFormat.getCurrentFormat(getSourceObject().getDataSource());
				final Combo ddlFormatCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY | SWT.DROP_DOWN);
				ddlFormatCombo.setToolTipText("DDL Format");
				for (DmDDLFormat format : DmDDLFormat.values()) {
					ddlFormatCombo.add(format.getTitle());
					if (format == ddlFormat) {
						ddlFormatCombo.select(ddlFormatCombo.getItemCount() - 1);
					}
				} 
				ddlFormatCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						for (DmDDLFormat format : DmDDLFormat.values()) {
							if (format.ordinal() == ddlFormatCombo.getSelectionIndex()) {
								getSourceObject().getDataSource().getContainer().getPreferenceStore()
										.setValue(DmConstants.PREF_KEY_DDL_FORMAT, format.name());
								refreshPart(this, true);
								break;
							}
						}
					}
				});
				return ddlFormatCombo;
			}
		});
	}

}
