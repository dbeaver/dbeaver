package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExasolStructureAssistant implements DBSStructureAssistant {

	private static final Log LOG = Log.getLog(ExasolStructureAssistant.class);

	/**
	 * Exasol Structure Assistant
	 * 
	 * @author Karl Griesser
	 */

	    // TODO DF: Work in progess

	    private final ExasolDataSource dataSource;

	    // -----------------
	    // Constructors
	    // -----------------
	    public ExasolStructureAssistant(ExasolDataSource dataSource)	    {
	    	
	        this.dataSource = dataSource;
	    }

	    // -----------------
	    // Method Interface
	    // -----------------

	    public DBSObjectType[] getSupportedObjectTypes()
	    {
	        return new DBSObjectType[] {
	                RelationalObjectType.TYPE_TABLE,
	                RelationalObjectType.TYPE_PROCEDURE,
	                RelationalObjectType.TYPE_CONSTRAINT
	                };

	    }

	    public DBSObjectType[] getHyperlinkObjectTypes()
	    {
	        return getSupportedObjectTypes();
	    }

	    public DBSObjectType[] getAutoCompleteObjectTypes()
	    {
	        return getSupportedObjectTypes();
	    }

		@Override
		public Collection<DBSObjectReference> findObjectsByMask(DBRProgressMonitor monitor, DBSObject parentObject,
				DBSObjectType[] objectTypes, String objectNameMask, boolean caseSensitive, boolean globalSearch,
				int maxResults) throws DBException {
			// TODO Auto-generated method stub
			return null;
		}



}
