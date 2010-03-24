package org.jkiss.dbeaver.ui.actions.sql;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.utils.DBeaverUtils;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;


public class CommitAction extends AbstractSQLAction
{

    public CommitAction()
    {
        setId(ICommandIds.CMD_COMMIT);
        setActionDefinitionId(ICommandIds.CMD_COMMIT);
        setImageDescriptor(DBeaverActivator.getImageDescriptor("/icons/sql/accept.png"));
        setText("Commit");
        setToolTipText("Commit");
    }

    protected void execute(SQLEditor editor)
    {
        try {
            editor.getSession().commit();
        } catch (DBException e) {
            DBeaverUtils.showErrorDialog(editor.getSite().getShell(), "Commit", "Error commiting data", e);
        }
    }

}