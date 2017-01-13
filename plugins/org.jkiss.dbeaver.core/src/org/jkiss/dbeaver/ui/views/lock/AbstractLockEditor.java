package org.jkiss.dbeaver.ui.views.lock;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.SinglePageDatabaseEditor;

/**
 * AbstractLockEditor
 */

public abstract class AbstractLockEditor extends SinglePageDatabaseEditor<IDatabaseEditorInput>
	{
	
	    private LockManagerViewer lockViewer;

	    public LockManagerViewer getLockViewer() {
	        return lockViewer;
	    }

	    @Override
	    public void dispose()
	    {
	        if (lockViewer != null) {
	            lockViewer.dispose();
	        }
	        super.dispose();
	    }

	    @Override
	    public void createPartControl(Composite parent) {
	    	final DBCExecutionContext executionContext = getExecutionContext();
	    	if (executionContext != null) {
        	 setPartName("Lock - " + executionContext.getDataSource().getContainer().getName());
        	 lockViewer = createLockViewer(executionContext, parent);
	         lockViewer.refreshLocks(null);
	    	}
	    }

	    protected abstract LockManagerViewer createLockViewer(DBCExecutionContext executionContext, Composite parent);

	    @Override
	    public void refreshPart(Object source, boolean force)
	    {
	        lockViewer.refreshLocks(null);
	    }

	}
