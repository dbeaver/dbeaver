/**
 * 
 */
package org.jkiss.dbeaver.ext.exasol.model.app;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.jkiss.dbeaver.ext.exasol.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.views.session.AbstractSessionEditor;
import org.jkiss.dbeaver.ui.views.session.SessionManagerViewer;
import org.jkiss.utils.CommonUtils;

/**
 * @author Karl
 *
 */
public class ExasolServerSessionEditor extends AbstractSessionEditor {

    private KillSessionAction killSessionAction = new KillSessionAction(false);
    private KillSessionAction terminateQueryAction = new KillSessionAction(true); 

    
	@Override
	public void createPartControl(Composite parent) {
        killSessionAction = new KillSessionAction(false);
        terminateQueryAction = new KillSessionAction(true);
        super.createPartControl(parent);
	}
	
	@Override
	protected SessionManagerViewer createSessionViewer(DBCExecutionContext executionContext, Composite parent) {
		return new SessionManagerViewer(this, parent, new ExasolServerSessionManager((ExasolDataSource) executionContext.getDataSource()))  {
			@Override
            protected void contributeToToolbar(DBAServerSessionManager sessionManager, IContributionManager contributionManager)
            {
                contributionManager.add(killSessionAction);
                contributionManager.add(terminateQueryAction);
                contributionManager.add(new Separator());
            }

            @Override
            protected void onSessionSelect(DBAServerSession session)
            {
                super.onSessionSelect(session);
                killSessionAction.setEnabled(session != null);
                terminateQueryAction.setEnabled(session != null && !CommonUtils.isEmpty(session.getActiveQuery()));
            }
		};
	}

	   private class KillSessionAction extends Action {
		   	private boolean killQuery;
	        public KillSessionAction(boolean killQuery)
	        {
	            super(
	            		killQuery ? ExasolMessages.editors_exasol_session_editor_title_kill_session_statement : ExasolMessages.editors_exasol_session_editor_title_kill_session, 
        				killQuery ?     UIUtils.getShardImageDescriptor(ISharedImages.IMG_ELCL_STOP) :
	            	                    DBeaverIcons.getImageDescriptor(UIIcon.SQL_DISCONNECT));
	            this.killQuery = killQuery;
	            
	        }

	        @Override
	        public void run()
	        {
	            final DBAServerSession session = getSessionsViewer().getSelectedSession();
	            final String action = ExasolMessages.editors_exasol_session_editor_action_kill;
	            if (UIUtils.confirmAction(getSite().getShell(), "Confirm kill session",
	                NLS.bind(ExasolMessages.editors_exasol_session_editor_confirm_action, action.toLowerCase(), session))) {
	                Map<String, Object> options = new HashMap<>();
	                getSessionsViewer().alterSession(getSessionsViewer().getSelectedSession(), Collections.singletonMap(ExasolServerSessionManager.PROP_KILL_QUERY, (Object) killQuery));
	            }
	        }
	    }


	   

}
