package org.jkiss.dbeaver.ext.oracle.editors;

import java.util.HashMap;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.lock.OracleLock;
import org.jkiss.dbeaver.ext.oracle.model.lock.OracleLockManager;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLock;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLockItem;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLockManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.views.lock.AbstractLockEditor;
import org.jkiss.dbeaver.ui.views.lock.LockManagerViewer;

public class OracleLockEditor extends AbstractLockEditor{
	
	   public static final String sidHold = "hsid";
	   public static final String sidWait = "wsid"; 

	@Override
	protected LockManagerViewer createLockViewer(DBCExecutionContext executionContext, Composite parent)
	{
		
	    DBAServerLockManager<DBAServerLock<?>, DBAServerLockItem> lockManager = (DBAServerLockManager) new OracleLockManager((OracleDataSource) executionContext.getDataSource());
		
		return new LockManagerViewer(this, parent, lockManager) {
			   @Override
	            protected void contributeToToolbar(DBAServerLockManager<DBAServerLock<?>, DBAServerLockItem> sessionManager, IContributionManager contributionManager)
	            {
	                contributionManager.add(new Separator());
	            }
	            @Override
	            protected void onLockSelect(final DBAServerLock<?> lock)
	            {
	                super.onLockSelect(lock);
	                if (lock != null ) {
	                	final OracleLock pLock = (OracleLock) lock;
	                	super.refreshDetail( new HashMap<String, Object>() {{ put(sidHold,new Integer(pLock.getHold_sid()));put(sidWait,new Integer(pLock.getWait_sid())); }});
	                }
	            }	
		};
	}

}
