package org.jkiss.dbeaver.ext.generic.model.meta;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Metadata reader
 */
public interface GenericMetadataReader {

    String fetchCatalogName(ResultSet dbResult) throws SQLException;

    String fetchSchemaName(ResultSet dbResult, String catalog) throws SQLException;

    String fetchTableType(ResultSet dbResult) throws SQLException;
}
