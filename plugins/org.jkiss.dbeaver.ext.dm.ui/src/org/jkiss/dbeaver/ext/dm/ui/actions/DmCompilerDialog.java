package org.jkiss.dbeaver.ext.dm.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.dm.model.source.DmSourceObject;
import org.jkiss.dbeaver.ext.dm.ui.DmUIActivator;
import org.jkiss.dbeaver.ext.dm.ui.internal.DmUIMessages;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.ObjectCompilerLogViewer;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;
import org.jkiss.utils.CommonUtils;

/**
 * Dm Compiler Dialog
 * 
 * @author caosw
 *
 */
public class DmCompilerDialog extends BaseDialog {

	private static final Log log = Log.getLog(DmCompilerDialog.class);

	private static final int COMPILE_ID = 1000;
	private static final int COMPILE_ALL_ID = 1001;

	private List<DmSourceObject> compileUnits;
	private TableViewer unitTable;

	private ObjectCompilerLogViewer compileLog;

	public DmCompilerDialog(Shell shell, List<DmSourceObject> compileUnits) {
		super(shell, DmUIMessages.views_dm_compiler_dialog_title, null);
		this.compileUnits = compileUnits;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Composite createDialogArea(Composite parent) {
		GridData gd;
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		{
			Composite unitsGroup = new Composite(composite, SWT.NONE);
			gd = new GridData(GridData.FILL_BOTH);
			gd.widthHint = 250;
			gd.heightHint = 200;
			gd.verticalIndent = 0;
			gd.horizontalIndent = 0;
			unitsGroup.setLayoutData(gd);
			unitsGroup.setLayout(new GridLayout(1, false));
			unitTable = new TableViewer(unitsGroup,
					SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
			{
				final Table table = unitTable.getTable();
				table.setLayoutData(new GridData(GridData.FILL_BOTH));
				table.setLinesVisible(true);
				table.setHeaderVisible(true);
			}
			ViewerColumnController columnController = new ViewerColumnController("DmCompilerDialog", unitTable);
			columnController.addColumn(DmUIMessages.views_dm_compiler_dialog_column_name, null, SWT.NONE, true, true,
					new CellLabelProvider() {

						@Override
						public void update(ViewerCell cell) {
							DBSObject unit = (DBSObject) cell.getElement();
							final DBNDatabaseNode node = DBNUtils.getNodeByObject(unit);
							if (node != null) {
								cell.setText(node.getNodeName());
								cell.setImage(DBeaverIcons.getImage(node.getNodeIconDefault()));
							} else {
								cell.setText(unit.toString());
							}
						}
					});

			columnController.addColumn(DmUIMessages.views_dm_compiler_dialog_column_type, null, SWT.NONE, true, true,
					new CellLabelProvider() {

						@Override
						public void update(ViewerCell cell) {
							DBSObject unit = (DBSObject) cell.getElement();
							final DBNDatabaseNode node = DBNUtils.getNodeByObject(unit);
							if (node != null) {
								cell.setText(node.getNodeType());
							} else {
								cell.setText("???");
							}
						}
					});
			columnController.createColumns();
			unitTable.addSelectionChangedListener(event -> {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				getButton(COMPILE_ID).setEnabled(!selection.isEmpty());

			});
			unitTable.addDoubleClickListener(event -> {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				if (!selection.isEmpty()) {
					DmSourceObject unit = (DmSourceObject) selection.getFirstElement();
					NavigatorHandlerObjectOpen.openEntityEditor(unit);
				}
			});
			unitTable.setContentProvider(new ListContentProvider());
			unitTable.setInput(compileUnits);
		}
		{
			Composite infoGroup = new Composite(composite, SWT.NONE);
			gd = new GridData(GridData.FILL_BOTH);
			gd.widthHint = 400;
			gd.heightHint = 200;
			gd.verticalIndent = 0;
			gd.horizontalIndent = 0;
			infoGroup.setLayoutData(gd);
			infoGroup.setLayout(new GridLayout(1, false));

			compileLog = new ObjectCompilerLogViewer(infoGroup, null, true);
		}

		return composite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, COMPILE_ID, DmUIMessages.views_dm_compiler_dialog_button_compile, false).setEnabled(false);
		createButton(parent, COMPILE_ALL_ID, DmUIMessages.views_dm_compiler_dialog_button_compile_all, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
	}

	@Override
	protected void okPressed() {
		super.okPressed();
	}

	@Override
	protected void buttonPressed(int buttonId) {
		final List<DmSourceObject> toCompile;
		if (buttonId == COMPILE_ID) {
			toCompile = ((IStructuredSelection) unitTable.getSelection()).toList();
		} else if (buttonId == COMPILE_ALL_ID) {
			toCompile = compileUnits;
		} else {
			toCompile = null;
		}

		if (!CommonUtils.isEmpty(toCompile)) {
			try {
				UIUtils.runInProgressService(monitor -> performCompilation(monitor, toCompile));
			} catch (InvocationTargetException e) {
				DBWorkbench.getPlatformUI().showError("Compile error", null, e.getTargetException());
			} catch (InterruptedException e) {
				// do nothing
			}
		} else {
			super.buttonPressed(buttonId);
		}
	}

	private void performCompilation(DBRProgressMonitor monitor, List<DmSourceObject> units) {
		compileLog.layoutLog();
		for (DmSourceObject unit : units) {
			if (monitor.isCanceled()) {
				break;
			}
			final String message = NLS.bind(DmUIMessages.views_dm_compiler_dialog_message_compile_unit,
					unit.getSourceType().name(), unit.getName());
			compileLog.info(message);
			boolean success = false;
			try {
				success = CompileHandler.compileUnit(monitor, compileLog, unit);
			} catch (DBCException e) {
				log.error("Compile error", e);
			}

			compileLog.info(!success ? DmUIMessages.views_dm_compiler_dialog_message_compilation_error
					: DmUIMessages.views_dm_compiler_dialog_message_compilation_success);
			compileLog.info(""); 
		}
	}

}
