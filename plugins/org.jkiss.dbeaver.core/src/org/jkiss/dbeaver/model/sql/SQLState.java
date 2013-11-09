/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.model.sql;

/**
 * Standard SQL states
 */
public enum SQLState {

	SQL_00000("00000", "Success"),
	SQL_01000("01000", "General warning"),
	SQL_01001("01001", "Cursor operation conflict"),
	SQL_01002("01002", "Disconnect error"),
	SQL_01004("01004", "Data truncated"),
	SQL_01006("01006", "Privilege not revoked"),
	SQL_01S00("01S00", "Invalid connection string attribute"),
	SQL_01S01("01S01", "Error in row"),
	SQL_01S02("01S02", "Option value changed"),
	SQL_07001("07001", "Wrong number of parameters"),
	SQL_07002("07002", "Mismatching parameters"),
	SQL_07003("07003", "Cursor specification cannot be executed"),
	SQL_07004("07004", "Missing parameters"),
	SQL_07005("07005", "Invalid cursor state"),
	SQL_07006("07006", "Restricted data type attribute violation"),
	SQL_07008("07008", "Invalid descriptor count"),
	SQL_08000("08000", "Connection exception"),
	SQL_08001("08001", "Unable to connect to the data source, e.g. invalid license key"),
	SQL_08002("08002", "Connection already in use"),
	SQL_08003("08003", "Connection not open"),
	SQL_08004("08004", "Data source rejected establishment of connection"),
	SQL_08007("08007", "Connection failure during transaction"),
	SQL_08900("08900", "Server lookup failed"),
	SQL_08S01("08S01", "Communication link failure"),
	SQL_21000("21000", "Cardinality violation"),
	SQL_21S01("21S01", "Insert value list does not match column list"),
	SQL_21S02("21S02", "Degree of derived table does not match column list"),
	SQL_22000("22000", "Data exception"),
	SQL_22001("22001", "String data, right truncation"),
	SQL_22003("22003", "Numeric value out of range"),
	SQL_22007("22007", "Invalid datetime format"),
	SQL_22012("22012", "Division by zero"),
	SQL_22018("22018", "Error in assignment"),
	SQL_22026("22026", "String data, length mismatch"),
	SQL_23000("23000", "Integrity constraint violation"),
	SQL_25000("25000", "Invalid transaction state"),
	SQL_25S02("25S02", "Transaction is still active"),
	SQL_25S03("25S03", "Transaction has been rolled back"),
	SQL_26000("26000", "Invalid SQL statement identifier"),
	SQL_28000("28000", "Invalid authorization specification"),
	SQL_34000("34000", "Invalid cursor name"),
	SQL_3C000("3C000", "Duplicate cursor name"),
	SQL_40000("40000", "Commit transaction resulted in rollback transaction"),
	SQL_40001("40001", "Serialization failure, e.g. timeout or deadlock"),
	SQL_42000("42000", "Syntax error or access rule violation"),
	SQL_42S01("42S01", "Base table or view already exists"),
	SQL_42S02("42S02", "Base table or view not found"),
	SQL_42S11("42S11", "Index already exists"),
	SQL_42S12("42S12", "Index not found"),
	SQL_42S21("42S21", "Column already exists"),
	SQL_42S22("42S22", "Column not found"),
	SQL_42S23("42S23", "No default for column"),
	SQL_44000("44000", "WITH CHECK OPTION violation"),
    ;

    private final String code;
    private final String description;

    private SQLState(String description, String code)
    {
        this.description = description;
        this.code = code;
    }

    public String getCode()
    {
        return code;
    }

    public String getDescription()
    {
        return description;
    }
}
