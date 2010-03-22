package org.jkiss.dbeaver.model.dbc;

import java.sql.SQLException;

/**
 * DBC LOB
 *
 * @author Serge Rider
 */
public interface DBCLOB {

    long getLength() throws DBCException;

}
