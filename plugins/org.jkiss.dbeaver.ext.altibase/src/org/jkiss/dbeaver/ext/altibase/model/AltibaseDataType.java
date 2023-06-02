/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.dbeaver.ext.generic.model.GenericDataType;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;

public class AltibaseDataType extends GenericDataType {

	private AltibaseDataTypeDomain dataTypeDomin;
	private boolean nullable;

    public AltibaseDataType(GenericStructContainer owner, AltibaseDataTypeDomain dataTypeDomin) {
    	 super(owner, dataTypeDomin.getValueType(), dataTypeDomin.getTypeName(), null, false, true, 0, 0, 0);
    	 this.dataTypeDomin = dataTypeDomin;
    }
    
	public AltibaseDataType(GenericStructContainer owner, AltibaseDataTypeDomain fieldType,
			String name, String remarks, boolean unsigned, boolean searchable,
			int precision, int minScale, int maxScale) {
		super(owner, fieldType.getValueType(), name, remarks, unsigned, searchable, precision, minScale, maxScale);
		
		this.dataTypeDomin = fieldType;
		this.nullable = nullable;
	}
	
	public int getDbTypeID() { return dataTypeDomin.getDbTypeID(); }
    
	/*
    static class TypeDesc {
    	final int dbDataType;
        final DBPDataKind dataKind;
        final int valueType;
        
        private TypeDesc(int dbDataType, DBPDataKind dataKind, int valueType)
        {
        	this.dbDataType = dbDataType;
            this.dataKind = dataKind;
            this.valueType = valueType;
        }
        
	    public int getTypeID() {
	        return valueType;
	    }
	
	    public int getValueType() {
	        return valueType;
	    }
	
	    public DBPDataKind getDataKind() {
	        return dataKind;
	    }
    }

    static final Map<String, TypeDesc> PREDEFINED_TYPES = new HashMap<>();
    static final Map<Integer, TypeDesc> PREDEFINED_TYPE_IDS = new HashMap<>();
    static  {
    	// char types
    	PREDEFINED_TYPES.put("CHAR", 	new TypeDesc(1,  DBPDataKind.STRING, Types.CHAR));
    	PREDEFINED_TYPES.put("VARCHAR", new TypeDesc(12, DBPDataKind.STRING, Types.VARCHAR));
    	PREDEFINED_TYPES.put("NCHAR", 	new TypeDesc(-8, DBPDataKind.STRING, Types.NCHAR));
    	PREDEFINED_TYPES.put("NVARCHAR",new TypeDesc(-9, DBPDataKind.STRING, Types.NVARCHAR));
    	// encrypted column data type
    	PREDEFINED_TYPES.put("ECHAR",	new TypeDesc(60, DBPDataKind.STRING, Types.BINARY));
    	PREDEFINED_TYPES.put("EVARCHAR",new TypeDesc(61, DBPDataKind.STRING, Types.BINARY));

    	// number types
    	PREDEFINED_TYPES.put("INTEGER", new TypeDesc(4,  	DBPDataKind.NUMERIC, Types.INTEGER));
    	PREDEFINED_TYPES.put("SMALLINT",new TypeDesc(5,  	DBPDataKind.NUMERIC, Types.SMALLINT));
        PREDEFINED_TYPES.put("BIGINT", 	new TypeDesc(-5, 	DBPDataKind.NUMERIC, Types.BIGINT));
        PREDEFINED_TYPES.put("REAL", 	new TypeDesc(7,  	DBPDataKind.NUMERIC, Types.REAL));
        PREDEFINED_TYPES.put("NUMBER", 	new TypeDesc(10002, DBPDataKind.NUMERIC, Types.NUMERIC));
        PREDEFINED_TYPES.put("NUMERIC", new TypeDesc(2, 	DBPDataKind.NUMERIC, Types.NUMERIC));
        PREDEFINED_TYPES.put("DOUBLE", 	new TypeDesc(8, 	DBPDataKind.NUMERIC, Types.DOUBLE));
        PREDEFINED_TYPES.put("FLOAT", 	new TypeDesc(6, 	DBPDataKind.NUMERIC, Types.FLOAT));
        
        // date & time
        PREDEFINED_TYPES.put("DATE", 	new TypeDesc(9,  	DBPDataKind.DATETIME, Types.TIMESTAMP));
        
        // binary
        PREDEFINED_TYPES.put("BIT", 	new TypeDesc(-5, 	DBPDataKind.BINARY, Types.BINARY));
        PREDEFINED_TYPES.put("VARBIT", 	new TypeDesc(-100, 	DBPDataKind.BINARY, Types.BINARY));
        PREDEFINED_TYPES.put("BYTE", 	new TypeDesc(20001, DBPDataKind.BINARY, Types.BINARY));
        PREDEFINED_TYPES.put("VARBYTE", new TypeDesc(20003, DBPDataKind.BINARY, Types.BINARY));
        PREDEFINED_TYPES.put("NIBBLE", 	new TypeDesc(20002, DBPDataKind.BINARY, Types.BINARY));
        PREDEFINED_TYPES.put("BINARY", 	new TypeDesc(-2, 	DBPDataKind.BINARY, Types.BINARY));
        
        PREDEFINED_TYPES.put("CLOB", 	new TypeDesc(40, 	DBPDataKind.BINARY, Types.CLOB));
        PREDEFINED_TYPES.put("BLOB", 	new TypeDesc(30, 	DBPDataKind.BINARY, Types.BLOB));
        PREDEFINED_TYPES.put("GEOMETRY",new TypeDesc(10003, DBPDataKind.BINARY, Types.BLOB));

        for (TypeDesc type : PREDEFINED_TYPES.values()) {
            PREDEFINED_TYPE_IDS.put(type.valueType, type);
        }
    }
    */
}
