package org.jkiss.dbeaver.ext.oceanbase.ui.editors;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.ui.views.session.AbstractSessionEditor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.session.MySQLSession;
import org.jkiss.dbeaver.ext.mysql.model.session.MySQLSessionManager;
import org.jkiss.dbeaver.ext.oceanbase.oracle.model.OceanbaseOracleDataSource;
import org.jkiss.dbeaver.ext.oceanbase.oracle.model.session.OceanbaseOracleServerSession;
import org.jkiss.dbeaver.ext.oceanbase.oracle.model.session.OceanbaseOracleServerSessionManager;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.views.session.SessionManagerViewer;
import org.jkiss.utils.CommonUtils;

public class OceanbaseSessionEditor extends AbstractSessionEditor {

	private KillSessionAction killSessionAction;
	private KillSessionAction terminateQueryAction;

	@Override
	public void createEditorControl(Composite parent) {
		killSessionAction = new KillSessionAction(false);
		terminateQueryAction = new KillSessionAction(true);
		super.createEditorControl(parent);
	}

	@Override
	protected SessionManagerViewer createSessionViewer(DBCExecutionContext executionContext, Composite parent) {
		String dataSource = executionContext.getDataSource().getClass().getSimpleName();
		if (dataSource.equals("OceanbaseMySQLDataSource")) {
			return new SessionManagerViewer<MySQLSession>(this, parent,
					new MySQLSessionManager((MySQLDataSource) executionContext.getDataSource())) {
				private boolean hideSleeping;

				@Override
				protected void contributeToToolbar(DBAServerSessionManager sessionManager,
						IContributionManager contributionManager) {
					contributionManager.add(killSessionAction);
					contributionManager.add(terminateQueryAction);
					contributionManager.add(new Separator());

					contributionManager
							.add(ActionUtils.makeActionContribution(new Action("Hide sleeping", Action.AS_CHECK_BOX) {
								{
									setToolTipText("Show only active connections");
									setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.HIDE_ALL_DETAILS));
									setChecked(hideSleeping);
								}

								@Override
								public void run() {
									hideSleeping = isChecked();
									refreshPart(OceanbaseSessionEditor.this, true);
								}
							}, true));

					contributionManager.add(new Separator());
				}

				@Override
				protected void onSessionSelect(DBAServerSession session) {
					super.onSessionSelect(session);
					killSessionAction.setEnabled(session != null);
					terminateQueryAction.setEnabled(session != null && !CommonUtils.isEmpty(session.getActiveQuery()));
				}

				@Override
				public Map<String, Object> getSessionOptions() {
					if (hideSleeping) {
						return Collections.singletonMap(MySQLSessionManager.OPTION_HIDE_SLEEPING, true);
					}
					return super.getSessionOptions();
				}

				@Override
				protected void loadSettings(IDialogSettings settings) {
					hideSleeping = CommonUtils.toBoolean(settings.get("hideSleeping"));
					super.loadSettings(settings);
				}

				@Override
				protected void saveSettings(IDialogSettings settings) {
					super.saveSettings(settings);
					settings.put("hideSleeping", hideSleeping);
				}
			};
		} else {
			return new SessionManagerViewer<OceanbaseOracleServerSession>(this, parent,
					new OceanbaseOracleServerSessionManager(
							(OceanbaseOracleDataSource) executionContext.getDataSource())) {
				@Override
				protected void contributeToToolbar(DBAServerSessionManager sessionManager,
						IContributionManager contributionManager) {
					contributionManager.add(killSessionAction);
					contributionManager.add(new Separator());
				}

				@Override
				protected void onSessionSelect(DBAServerSession session) {
					super.onSessionSelect(session);
					killSessionAction.setEnabled(session != null);
				}

			};
		}
	}

	private class KillSessionAction extends Action {
		private boolean killQuery;

		public KillSessionAction(boolean killQuery) {
			super(killQuery ? "Terminate" : "Kill",
					killQuery ? UIUtils.getShardImageDescriptor(ISharedImages.IMG_ELCL_STOP)
							: DBeaverIcons.getImageDescriptor(UIIcon.SQL_DISCONNECT));
			this.killQuery = killQuery;
		}

		@Override
		public void run() {
			final List<DBAServerSession> sessions = getSessionsViewer().getSelectedSessions();
			if (sessions != null && UIUtils.confirmAction(getSite().getShell(), this.getText(),
					NLS.bind("confirm", getText(), sessions))) {
				getSessionsViewer().alterSessions(sessions,
						Collections.singletonMap(MySQLSessionManager.PROP_KILL_QUERY, killQuery));
			}
		}
	}
}
