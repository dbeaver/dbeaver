/*
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.session.DB2ServerSessionManager;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.editors.SinglePageDatabaseEditor;
import org.jkiss.dbeaver.ui.views.session.SessionManagerViewer;

import java.util.HashMap;
import java.util.Map;

/**
 * DB2SessionEditor
 */
public class DB2SessionEditor extends SinglePageDatabaseEditor<IDatabaseEditorInput> {
   private SessionManagerViewer    sessionsViewer;
   private DisconnectSessionAction killSessionAction;
   private DisconnectSessionAction disconnectSessionAction;

   @Override
   public void dispose() {
      sessionsViewer.dispose();
      super.dispose();
   }

   @Override
   public void createPartControl(Composite parent) {
      killSessionAction = new DisconnectSessionAction(true);
      disconnectSessionAction = new DisconnectSessionAction(false);
      sessionsViewer = new SessionManagerViewer(this, parent, new DB2ServerSessionManager((DB2DataSource) getDataSource())) {
         @Override
         protected void contributeToToolbar(DBAServerSessionManager sessionManager, ToolBarManager toolBar) {
            toolBar.add(killSessionAction);
            toolBar.add(disconnectSessionAction);
            toolBar.add(new Separator());
         }

         @Override
         protected void onSessionSelect(DBAServerSession session) {
            super.onSessionSelect(session);
            killSessionAction.setEnabled(session != null);
            disconnectSessionAction.setEnabled(session != null);
         }
      };

      sessionsViewer.refreshSessions();
   }

   @Override
   public void refreshPart(Object source, boolean force) {
      sessionsViewer.refreshSessions();
   }

   private class DisconnectSessionAction extends Action {
      private final boolean kill;

      public DisconnectSessionAction(boolean kill) {
         super(kill ? DB2Messages.editors_db2_session_editor_title_kill_session
                  : DB2Messages.editors_db2_session_editor_title_disconnect_session, kill ? DBIcon.REJECT.getImageDescriptor()
                  : DBIcon.SQL_DISCONNECT.getImageDescriptor());
         this.kill = kill;
      }

      @Override
      public void run() {
         final DBAServerSession session = sessionsViewer.getSelectedSession();
         final String action = (kill ? DB2Messages.editors_db2_session_editor_action_kill
                  : DB2Messages.editors_db2_session_editor_action_disconnect)
                  + DB2Messages.editors_db2_session_editor_action__session;
         ConfirmationDialog dialog = new ConfirmationDialog(getSite().getShell(),
                                                            action,
                                                            null,
                                                            NLS.bind(DB2Messages.editors_db2_session_editor_confirm_action,
                                                                     action.toLowerCase(), session),
                                                            MessageDialog.CONFIRM,
                                                            new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
                                                            0,
                                                            DB2Messages.editors_db2_session_editor_confirm_title,
                                                            false);
         if (dialog.open() == IDialogConstants.YES_ID) {
            Map<String, Object> options = new HashMap<String, Object>();
            if (kill) {
               options.put(DB2ServerSessionManager.PROP_KILL_SESSION, kill);
            }
            if (dialog.getToggleState()) {
               options.put(DB2ServerSessionManager.PROP_IMMEDIATE, true);
            }
            sessionsViewer.alterSession(session, options);
         }
      }
   }

}