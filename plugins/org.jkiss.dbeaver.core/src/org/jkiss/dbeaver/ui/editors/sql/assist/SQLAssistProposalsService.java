
/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.assist;

import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

import java.util.List;


public interface SQLAssistProposalsService {

    /**
     * Gets the <code>ISQLEditorConnectionInfo</code> used to provide content assist.
     *
     * @return the current <code>ISQLEditorConnectionInfo</code> object
     */
    DBSDataSourceContainer getSQLEditorConnectionInfo();

    /**
     * Sets the <code>ISQLEditorConnectionInfo</code> used to provide content assist.
     */
    public void setSQLEditorConnectionInfo(DBSDataSourceContainer connectionInfo);

    /**
	 * Populates the list database of database object proposals (schemas,
	 * tables, columns) using given list of tokens (DB identifiers) indicating
	 * the start of the expression for which the user wants DB proposals. For
	 * example, if the user provides the list (MYSCHEMA, TABLE1), the list of
	 * proposals will be the columns of table MYSCHEMA.TABLE1. Retrieve the list
	 * using getDBProposals().
	 *
	 * @return true if database objects have loaded, for example as a result of
	 *         reestablishing a connection, otherwise false
	 */
    public boolean populate( SQLAssistProposalsRequest request );

    /**
     * Gets the list of <code>SQLDBProposal</code> objects for the content assist proposals.
     * Call populate to populate this list.
     *
     * @return the list of proposals
     */
    List<SQLAssistProposal> getDBProposals();
}