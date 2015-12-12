/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mysql.edit;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.mysql.views.MySQLCreateDatabaseDialog;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.utils.CommonUtils;

/**
 * MySQLCatalogManager
 */
public class MySQLCatalogManager extends SQLObjectEditor<MySQLCatalog, MySQLDataSource> implements DBEObjectRenamer<MySQLCatalog> {

    @Override
    public long getMakerOptions()
    {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Nullable
    @Override
    public DBSObjectCache<MySQLDataSource, MySQLCatalog> getObjectsCache(MySQLCatalog object)
    {
        return object.getDataSource().getCatalogCache();
    }

    @Override
    protected MySQLCatalog createDatabaseObject(DBECommandContext context, MySQLDataSource parent, Object copyFrom)
    {
        MySQLCreateDatabaseDialog dialog = new MySQLCreateDatabaseDialog(DBeaverUI.getActiveWorkbenchShell(), parent);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }
        String schemaName = dialog.getName();
        MySQLCatalog newCatalog = new MySQLCatalog(parent, null);
        newCatalog.setName(schemaName);
        newCatalog.setDefaultCharset(dialog.getCharset());
        newCatalog.setDefaultCollation(dialog.getCollation());
        return newCatalog;
    }

    @Override
    protected DBEPersistAction[] makeObjectCreateActions(ObjectCreateCommand command)
    {
        final MySQLCatalog catalog = command.getObject();
        final StringBuilder script = new StringBuilder("CREATE SCHEMA `" + catalog.getName() + "`");
        if (catalog.getDefaultCharset() != null) {
            script.append("\nDEFAULT CHARACTER SET ").append(catalog.getDefaultCharset().getName());
        }
        if (catalog.getDefaultCollation() != null) {
            script.append("\nDEFAULT COLLATE ").append(catalog.getDefaultCollation().getName());
        }
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction("Create schema", script.toString()) //$NON-NLS-2$
        };
    }

    @Override
    protected DBEPersistAction[] makeObjectDeleteActions(ObjectDeleteCommand command)
    {
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction("Drop schema", "DROP SCHEMA `" + command.getObject().getName() + "`") //$NON-NLS-2$
        };
    }

    @Override
    public void renameObject(DBECommandContext commandContext, MySQLCatalog catalog, String newName) throws DBException
    {
        throw new DBException("Direct database rename is not yet implemented in MySQL. You should use export/import functions for that.");
        //super.addCommand(new CommandRenameCatalog(newName), null);
        //saveChanges(monitor);
    }

/*
    private class CommandRenameCatalog extends DBECommandAbstract<MySQLCatalog> {
        private String newName;

        protected CommandRenameCatalog(MySQLCatalog catalog, String newName)
        {
            super(catalog, "Rename catalog");
            this.newName = newName;
        }
        public DBEPersistAction[] getPersistActions()
        {
            return new DBEPersistAction[] {
                new SQLDatabasePersistAction("Rename catalog", "RENAME SCHEMA " + getObject().getName() + " TO " + newName)
            };
        }

        @Override
        public void updateModel()
        {
            getObject().setName(newName);
            getObject().getDataSource().getContainer().fireEvent(
                new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, getObject()));
        }
    }
*/

    /*
http://www.artfulsoftware.com/infotree/queries.php#112
Rename Database
It's sometimes necessary to rename a database. MySQL 5.0 has no command for it. Simply bringing down the server to rename a database directory is not safe. MySQL 5.1.7 introduced a RENAME DATABASE command, but the command left several unchanged database objects behind, and was found to lose data, so it was dropped in 5.1.23.

It seems a natural for a stored procedure using dynamic (prepared) statements. PREPARE supports CREATE | RENAME TABLE. As precautions:

    Before calling the sproc, the new database must have been created.
    The procedure refuses to rename the mysql database.
    The old database is left behind, minus what was moved.


DROP PROCEDURE IF EXISTS RenameDatabase;
DELIMITER go
CREATE PROCEDURE RenameDatabase( oldname CHAR (64), newname CHAR(64) )
BEGIN
  DECLARE version CHAR(32);
  DECLARE sname CHAR(64) DEFAULT NULL;
  DECLARE rows INT DEFAULT 1;
  DECLARE changed INT DEFAULT 0;
  IF STRCMP( oldname, 'mysql' ) <> 0 THEN
    REPEAT
      SELECT table_name INTO sname
      FROM information_schema.tables AS t
      WHERE t.table_type='BASE TABLE' AND t.table_schema = oldname
      LIMIT 1;
      SET rows = FOUND_ROWS();
      IF rows = 1 THEN
        SET @scmd = CONCAT( 'RENAME TABLE `', oldname, '`.`', sname,
                            '` TO `', newname, '`.`', sname, '`' );
        PREPARE cmd FROM @scmd;
        EXECUTE cmd;
        DEALLOCATE PREPARE cmd;
        SET changed = 1;
      END IF;
    UNTIL rows = 0 END REPEAT;
    IF changed > 0 THEN
      SET @scmd = CONCAT( "UPDATE mysql.db SET Db = '",
                          newname,
                          "' WHERE Db = '", oldname, "'" );
      PREPARE cmd FROM @scmd;
      EXECUTE cmd;
      DROP PREPARE cmd;
      SET @scmd = CONCAT( "UPDATE mysql.proc SET Db = '",
                          newname,
                          "' WHERE Db = '", oldname, "'" );
      PREPARE cmd FROM @scmd;
      EXECUTE cmd;
      DROP PREPARE cmd;
      SELECT version() INTO version;
      IF version >= '5.1.7' THEN
        SET @scmd = CONCAT( "UPDATE mysql.event SET db = '",
                            newname,
                            "' WHERE db = '", oldname, "'" );
        PREPARE cmd FROM @scmd;
        EXECUTE cmd;
        DROP PREPARE cmd;
      END IF;
      SET @scmd = CONCAT( "UPDATE mysql.columns_priv SET Db = '",
                          newname,
                          "' WHERE Db = '", oldname, "'" );
      PREPARE cmd FROM @scmd;
      EXECUTE cmd;
      DROP PREPARE cmd;
      FLUSH PRIVILEGES;
    END IF;
  END IF;
END;
go
DELIMITER ;

     */

}

