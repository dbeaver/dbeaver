package org.jkiss.dbeaver.ui.controls.resultset;



import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSAttributeEnumerable;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.data.editors.ReferenceValueEditor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

public class CustomCheckboxCombo extends Composite {
	



	 public static final int MAX_MULTI_VALUES = 1000;
	 public static final String MULTI_KEY_LABEL = "...";
	@NotNull
    private final ResultSetViewer viewer;
    @NotNull
    private final DBDAttributeBinding attr;
    @NotNull
    private final ResultSetRow[] rows;
    @NotNull
    private final DBCLogicalOperator operator = DBCLogicalOperator.IN;
    private CheckboxTableViewer table;
    private String filterPattern;
    private KeyLoadJob loadJob;
    private Shell shell;


	
    /**
    *
    * Constructs a new instance of this class given its parent, a style value describing its behavior and appearance, and default text
    *
    * The style value is either one of the style constants defined in class SWT which is applicable to instances of this class, or must be built by bitwise OR'ing together (that is, using the int "|" operator) two or more of those SWT style constants. The class description lists the style constants that are applicable to the class. Style bits are also inherited from superclasses.
    * 
    * The default text is displayed when no options are selected.
    * 
    * @param parent a composite control which will be the parent of the new instance (cannot be null)
    * @param style the style of control to construct
    * @param trigger the Control listening for a mousedown to show the combo
    * @throws IllegalArgumentException if the parent is null
    * @throws IllegalArgumentException if the defaultText is null
    * @throws SWTException if not called from the thread that created the parent
    * @since version 1.0.0.0
    */
	public CustomCheckboxCombo(Composite parent, int style, @Nullable Control trigger,@NotNull ResultSetViewer viewer, @NotNull DBDAttributeBinding attr, @NotNull ResultSetRow[] rows) {
		super(parent, style);		
		this.attr = attr;
		this.rows = rows;
		this.viewer = viewer;
		init(trigger);
	}
	
	private void init(Control trigger) {

		initFloatShell();
		if(trigger != null) {
			trigger.addListener(SWT.MouseDown, e -> {
				setVisible(true);
		    });
		}
	    
	}


	public void setLocation(Point location) {
		shell.setLocation(location);
	}
	
	public void setVisible(boolean v) {		
		shell.setVisible(v);
	}
	

	
	private void initFloatShell() {		
		shell = new Shell(CustomCheckboxCombo.this.getShell(), SWT.BORDER);
		shell.setLayout(new GridLayout());
	   
		
		
	   	shell.addListener(SWT.Deactivate, e-> {
       	 if (shell != null && !shell.isDisposed()) {
    		 shell.setVisible(false);
    		 //shell.close(); 	
    		 shell.dispose();
    	 }
	   	});
		
	   	createMultiValueSelector(shell);
	   	shell.pack();
	   	
	   	setVisible(false);
	}
	

	private void createMultiValueSelector(Composite composite) {
        table = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.MULTI | SWT.CHECK | SWT.FULL_SELECTION);
        table.getTable().setLinesVisible(true);
        table.getTable().setHeaderVisible(false);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        gd.heightHint = 300;
        table.getTable().setLayoutData(gd);
        table.setContentProvider(new ListContentProvider());

        
        
        ViewerColumnController columnController = new ViewerColumnController(getClass().getName(), table);
        columnController.addColumn("Value", "Value", SWT.LEFT, true, true, new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return attr.getValueHandler().getValueDisplayString(attr, ((DBDLabelValuePair)element).getValue(), DBDDisplayFormat.UI);
            }
        });
     
        columnController.createColumns();

        table.getTable().getColumn(0).setWidth(300);
        
        MenuManager menuMgr = new MenuManager();
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                UIUtils.fillDefaultTableContextMenu(manager, table.getTable());
                manager.add(new Separator());
                manager.add(new Action("Select &All") {
                    @Override
                    public void run() {
                        for (DBDLabelValuePair row : getMultiValues()) {
                            table.setChecked(row, true);
                        }
                    }
                });
                manager.add(new Action("Select &None") {
                    @Override
                    public void run() {
                        for (DBDLabelValuePair row : getMultiValues()) {
                            table.setChecked(row, false);
                        }
                    }
                });
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        table.getTable().setMenu(menuMgr.createContextMenu(table.getTable()));

        if (attr.getDataKind() == DBPDataKind.STRING) {
            // Create filter text
            final Text valueFilterText = new Text(composite, SWT.BORDER);
            valueFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            valueFilterText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    filterPattern = valueFilterText.getText();
                    if (filterPattern.isEmpty()) {
                        filterPattern = null;
                    }
                    loadValues();
                }
            });
        }

        filterPattern = null;
        loadValues();
    }

  
	private Collection<DBDLabelValuePair> getMultiValues() {
        return (Collection<DBDLabelValuePair>)table.getInput();
    }
	
	
	private void loadValues() {
        if (loadJob != null) {
            if (loadJob.getState() == Job.RUNNING) {
                loadJob.cancel();
            }
            loadJob.schedule(100);
            return;
        }
        // Load values
        final DBSEntityReferrer enumerableConstraint = ReferenceValueEditor.getEnumerableConstraint(attr);
       
        loadAttributeEnum((DBSAttributeEnumerable) attr.getEntityAttribute());
       
    }

	private void loadAttributeEnum(final DBSAttributeEnumerable attributeEnumerable) {
        //table.getTable().getColumn(1).setText("Count");
        loadJob = new KeyLoadJob("Load '" + attr.getName() + "' values") {
            @Override
            protected Collection<DBDLabelValuePair> readEnumeration(DBCSession session) throws DBException {
                return attributeEnumerable.getValueEnumeration(session, filterPattern, MAX_MULTI_VALUES);
            }
        };
        loadJob.schedule();
    }

	 private void loadMultiValueList(@NotNull Collection<DBDLabelValuePair> values) {
	        Pattern pattern = null;
	        if (!CommonUtils.isEmpty(filterPattern)) {
	            pattern = Pattern.compile(SQLUtils.makeLikePattern("%" + filterPattern + "%"), Pattern.CASE_INSENSITIVE);
	        }

	        // Get all values from actual RSV data
	        boolean hasNulls = false;
	        java.util.Map<Object, DBDLabelValuePair> rowData = new HashMap<>();
	        for (DBDLabelValuePair pair : values) {
	            final DBDLabelValuePair oldLabel = rowData.get(pair.getValue());
	            if (oldLabel != null) {
	                // Duplicate label for single key - may happen in case of composite foreign keys
	                String multiLabel = oldLabel.getLabel() + "," + pair.getLabel();
	                if (multiLabel.length() > 200) {
	                    multiLabel = multiLabel.substring(0, 200) + MULTI_KEY_LABEL;
	                }
	                rowData.put(pair.getValue(), new DBDLabelValuePair(multiLabel, pair.getValue()));
	            } else{
	                rowData.put(pair.getValue(), pair);
	            }
	        }
	        // Add values from fetched rows
	        for (ResultSetRow row : viewer.getModel().getAllRows()) {
	            Object cellValue = viewer.getModel().getCellValue(attr, row);
	            if (DBUtils.isNullValue(cellValue)) {
	                hasNulls = true;
	                continue;
	            }
	            if (!rowData.containsKey(cellValue)) {
	                String itemString = attr.getValueHandler().getValueDisplayString(attr, cellValue, DBDDisplayFormat.UI);
	                rowData.put(cellValue, new DBDLabelValuePair(itemString, cellValue));
	            }
	        }

	        java.util.List<DBDLabelValuePair> sortedList = new ArrayList<>(rowData.values());
	        Collections.sort(sortedList);
	        if (pattern != null) {
	            for (Iterator<DBDLabelValuePair> iter = sortedList.iterator(); iter.hasNext(); ) {
	                final DBDLabelValuePair valuePair = iter.next();
	                String itemString = attr.getValueHandler().getValueDisplayString(attr, valuePair.getValue(), DBDDisplayFormat.UI);
	                if (!pattern.matcher(itemString).matches() && (valuePair.getLabel() == null || !pattern.matcher(valuePair.getLabel()).matches())) {
	                    iter.remove();
	                }
	            }
	        }
	        Collections.sort(sortedList);
	        if (hasNulls) {
	            sortedList.add(0, new DBDLabelValuePair(DBValueFormatting.getDefaultValueDisplayString(null, DBDDisplayFormat.UI), null));
	        }

	        Set<Object> checkedValues = new HashSet<>();
	        for (ResultSetRow row : rows) {
	            Object value = viewer.getModel().getCellValue(attr, row);
	            checkedValues.add(value);
	        }

	        table.setInput(sortedList);
	        DBDLabelValuePair firstVisibleItem = null;
	        for (DBDLabelValuePair row : sortedList) {
	            Object cellValue = row.getValue();

	            if (checkedValues.contains(cellValue)) {
	                table.setChecked(row, true);
	                if (firstVisibleItem == null) {
	                    firstVisibleItem = row;
	                }
	            }
	        }
	        ViewerColumnController.getFromControl(table.getTable()).repackColumns();
	        if (firstVisibleItem != null) {
	            final Widget item = table.testFindItem(firstVisibleItem);
	            if (item != null) {
	                table.getTable().setSelection((TableItem) item);
	                table.getTable().showItem((TableItem) item);
	            }
	        }
	    }

   


	private abstract class KeyLoadJob extends AbstractJob {
        protected KeyLoadJob(String name) {
            super(name);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            final DBCExecutionContext executionContext = viewer.getExecutionContext();
            if (executionContext == null) {
                return Status.OK_STATUS;
            }
            try (DBCSession session = DBUtils.openUtilSession(monitor, executionContext.getDataSource(), "Read value enumeration")) {
                final Collection<DBDLabelValuePair> valueEnumeration = readEnumeration(session);
                if (valueEnumeration == null) {
                    return Status.OK_STATUS;
                } else {
                    populateValues(valueEnumeration);
                }
            } catch (DBException e) {
                populateValues(Collections.<DBDLabelValuePair>emptyList());
                return GeneralUtils.makeExceptionStatus(e);
            }
            return Status.OK_STATUS;
        }

        @Nullable
        protected abstract Collection<DBDLabelValuePair> readEnumeration(DBCSession session) throws DBException;

        protected void populateValues(@NotNull final Collection<DBDLabelValuePair> values) {
            DBeaverUI.asyncExec(new Runnable() {
                @Override
                public void run() {
                    loadMultiValueList(values);
                }
            });
        }
    }

	
}
