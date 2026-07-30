/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.polardbx.mysql.model;

import java.util.*;

/**
 * Function Definition Parser for PolarDB-X
 * 
 * This class parses SHOW CREATE FUNCTION results to extract function parameter information.
 * The implementation is based on the existing logic from MySQL Connector/J's DatabaseMetaData class,
 * specifically the TypeDescriptor and getCallStmtParameterTypes methods.
 * 
 * @author DBeaver Team
 */
public class FunctionDefinitionParser {

    /**
     * Function parameter information class
     */
    public static class ParameterInfo {
        private final String parameterName;
        private final int length;
        private final String dataType;
        private final boolean isReturnParameter;
        private final int scale;
        
        public ParameterInfo(String parameterName, int length, String dataType, boolean isReturnParameter, int scale) {
            this.parameterName = parameterName;
            this.length = length;
            this.dataType = dataType;
            this.isReturnParameter = isReturnParameter;
            this.scale = scale;
        }
        
        public String getParameterName() { 
            return parameterName; 
        }
        
        public int getLength() { 
            return length; 
        }
        
        public String getDataType() { 
            return dataType; 
        }
        
        public boolean isReturnParameter() { 
            return isReturnParameter; 
        }
        
        public int getScale() {
            return scale;
        }
        
        @Override
        public String toString() {
            return String.format("ParameterInfo{name='%s', length=%d, type='%s', scale=%d, isReturn=%s}", 
                               parameterName, length, dataType, scale, isReturnParameter);
        }
    }

    /**
     * Parse SHOW CREATE FUNCTION result to extract function parameter information
     * Based on DatabaseMetaData.getCallStmtParameterTypes logic
     * 
     * @param functionDefinition The function definition string from SHOW CREATE FUNCTION
     * @return List of parameter information including parameter names and lengths
     */
    public static Map<String, ParameterInfo> parseFunctionDefinition(String functionDefinition) {
        Map<String, ParameterInfo> parameters = new LinkedHashMap<>();

        if (functionDefinition == null || functionDefinition.trim().isEmpty()) {
            return parameters;
        }
        
        try {
            // Based on DatabaseMetaData.getCallStmtParameterTypes logic
            // Clean up whitespace characters like the original implementation
            String procedureDef = functionDefinition.replaceAll("[\\t\\n\\x0B\\f\\r]", " ");
            
            // 1. Parse return type - based on existing RETURNS parsing logic
            ParameterInfo returnParam = parseReturnTypeUsingOriginalLogic(procedureDef);
            if (returnParam != null) {
                parameters.put("return", returnParam);
            }

            // 2. Parse input parameters - based on existing parameter parsing logic
            Map<String, ParameterInfo> inputParams = parseInputParametersUsingOriginalLogic(procedureDef);
            parameters.putAll(inputParams);
            
        } catch (Exception e) {
            // Log error but don't throw exception to maintain compatibility
            System.err.println("Error parsing function definition: " + e.getMessage());
        }

        return parameters;
    }

    /**
     * Parse return type based on DatabaseMetaData existing logic
     * Based on DatabaseMetaData.java line 1538 logic
     */
    private static ParameterInfo parseReturnTypeUsingOriginalLogic(String procedureDef) {
        int returnsIndex = procedureDef.toUpperCase().indexOf("RETURNS");
        if (returnsIndex != -1) {
            int declarationStart = returnsIndex + "RETURNS".length();
            
            // Find end of RETURNS clause
            int endReturnsDef = findEndOfReturnsClause(procedureDef, declarationStart);
            
            if (endReturnsDef != -1) {
                String returnsDefn = procedureDef.substring(declarationStart, endReturnsDef).trim();

                // Use existing TypeDescriptor parsing logic
                TypeInfo typeInfo = parseTypeUsingOriginalLogic(returnsDefn);

                return new ParameterInfo(
                    "RETURN",  // Standard name for return parameter
                    typeInfo.columnSize,
                    typeInfo.typeName,
                    true,  // Mark as return parameter
                    typeInfo.decimalDigits
                );
            }
        }
        
        return null;
    }

    /**
     * Find end position of RETURNS clause
     */
    private static int findEndOfReturnsClause(String procedureDef, int startPos) {
        // Look for possible end keywords
        String[] endKeywords = {"DETERMINISTIC", "NOT DETERMINISTIC", "READS SQL DATA", 
                               "MODIFIES SQL DATA", "NO SQL", "CONTAINS SQL", "SQL SECURITY", 
                               "COMMENT", "LANGUAGE SQL", "BEGIN", "AS"};
        
        int minEndPos = procedureDef.length();
        
        for (String keyword : endKeywords) {
            int keywordPos = procedureDef.toUpperCase().indexOf(keyword, startPos);
            if (keywordPos != -1 && keywordPos < minEndPos) {
                minEndPos = keywordPos;
            }
        }
        
        return minEndPos;
    }

    /**
     * Parse input parameters based on DatabaseMetaData existing logic
     * Based on DatabaseMetaData.java lines 1543-1550 logic
     */
    private static Map<String, ParameterInfo> parseInputParametersUsingOriginalLogic(String procedureDef) {
        Map<String, ParameterInfo> parameters = new LinkedHashMap<>();
        
        int openParenIndex = procedureDef.indexOf('(');
        int endOfParamDeclarationIndex = findMatchingCloseParen(procedureDef, openParenIndex);
        
        if (openParenIndex == -1 || endOfParamDeclarationIndex == -1) {
            return parameters; // No parameters
        }
        
        // Extract parameter list
        String parameterList = procedureDef.substring(openParenIndex + 1, endOfParamDeclarationIndex);
        
        if (parameterList.trim().isEmpty()) {
            return parameters; // No parameter function
        }
        
        // Split parameters based on existing logic
        List<String> parameterStrings = splitParametersUsingOriginalLogic(parameterList);
        
        for (String paramStr : parameterStrings) {
            ParameterInfo param = parseParameterUsingOriginalLogic(paramStr.trim());
            if (param != null) {
                // Store with lowercase key for consistent lookup
                String key = param.getParameterName().toLowerCase();
                parameters.put(key, param);
            }
        }
        
        return parameters;
    }

    /**
     * Split parameter list based on existing logic
     * Simulates DatabaseMetaData parameter splitting logic without regex
     */
    private static List<String> splitParametersUsingOriginalLogic(String parameterList) {
        List<String> parameters = new ArrayList<>();
        
        // Based on existing string processing logic, no regex
        int start = 0;
        int parenCount = 0;
        boolean inQuotes = false;
        char quoteChar = 0;
        
        for (int i = 0; i < parameterList.length(); i++) {
            char c = parameterList.charAt(i);
            
            if (!inQuotes) {
                if (c == '\'' || c == '"' || c == '`') {
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == '(') {
                    parenCount++;
                } else if (c == ')') {
                    parenCount--;
                } else if (c == ',' && parenCount == 0) {
                    // Found parameter separator
                    String param = parameterList.substring(start, i).trim();
                    if (!param.isEmpty()) {
                        parameters.add(param);
                    }
                    start = i + 1;
                }
            } else {
                if (c == quoteChar) {
                    inQuotes = false;
                }
            }
        }
        
        // Add last parameter
        String lastParam = parameterList.substring(start).trim();
        if (!lastParam.isEmpty()) {
            parameters.add(lastParam);
        }
        
        return parameters;
    }

    /**
     * Parse single parameter based on existing logic
     */
    private static ParameterInfo parseParameterUsingOriginalLogic(String parameterDefinition) {
        if (parameterDefinition == null || parameterDefinition.trim().isEmpty()) {
            return null;
        }
        
        // Remove direction modifiers - based on existing logic
        String cleanParam = parameterDefinition;
        if (cleanParam.toUpperCase().startsWith("IN ")) {
            cleanParam = cleanParam.substring(3).trim();
        } else if (cleanParam.toUpperCase().startsWith("OUT ")) {
            cleanParam = cleanParam.substring(4).trim();
        } else if (cleanParam.toUpperCase().startsWith("INOUT ")) {
            cleanParam = cleanParam.substring(6).trim();
        }
        
        // Split parameter name and type definition - using existing string processing
        int firstSpaceIndex = cleanParam.indexOf(' ');
        if (firstSpaceIndex == -1) {
            return null; // Invalid format
        }
        
        String parameterName = cleanParam.substring(0, firstSpaceIndex).trim();
        String typeDefinition = cleanParam.substring(firstSpaceIndex + 1).trim();
        
        // Remove backticks from parameter name - based on existing logic
        if (parameterName.startsWith("`") && parameterName.endsWith("`")) {
            parameterName = parameterName.substring(1, parameterName.length() - 1);
        }
        
        // Parse type definition
        TypeInfo typeInfo = parseTypeUsingOriginalLogic(typeDefinition);
        
        return new ParameterInfo(
            parameterName,
            typeInfo.columnSize,
            typeInfo.typeName,
            false,  // Input parameter
            typeInfo.decimalDigits
        );
    }

    /**
     * Type information internal class - based on DatabaseMetaData.TypeDescriptor
     */
    private static class TypeInfo {
        final String typeName;
        final int columnSize;
        final int decimalDigits;
        
        TypeInfo(String typeName, int columnSize, int decimalDigits) {
            this.typeName = typeName;
            this.columnSize = columnSize;
            this.decimalDigits = decimalDigits;
        }
    }

    /**
     * Parse type based on DatabaseMetaData.TypeDescriptor existing logic
     * Completely copies existing type parsing logic, no regex
     * Based on DatabaseMetaData.java lines 240-410
     */
    private static TypeInfo parseTypeUsingOriginalLogic(String typeInfo) {
        if (typeInfo == null || typeInfo.trim().isEmpty()) {
            return new TypeInfo("UNKNOWN", 255, 0);
        }

        // Normalize type info - based on existing logic
        String normalizedType = typeInfo.trim().toUpperCase();

        // Determine base MySQL type - based on DatabaseMetaData.TypeDescriptor logic
        String baseTypeName = extractBaseTypeName(normalizedType);
        
        // Parse based on type - completely based on existing switch logic
        int columnSize = 0;
        int decimalDigits = 0;
        
        // Based on DatabaseMetaData.java lines 240-410 logic
        if (baseTypeName.equals("FLOAT") || baseTypeName.equals("DOUBLE")) {
            // Based on lines 254-267 logic
            if (typeInfo.indexOf(",") != -1) {
                String precisionStr = typeInfo.substring(typeInfo.indexOf("(") + 1, typeInfo.indexOf(",")).trim();
                String scaleStr = typeInfo.substring(typeInfo.indexOf(",") + 1, typeInfo.indexOf(")")).trim();
                try {
                    columnSize = Integer.parseInt(precisionStr);
                    decimalDigits = Integer.parseInt(scaleStr);
                } catch (NumberFormatException e) {
                    columnSize = 12; // Default value
                    decimalDigits = 0;
                }
            } else if (typeInfo.indexOf("(") != -1) {
                String sizeStr = typeInfo.substring(typeInfo.indexOf("(") + 1, typeInfo.indexOf(")")).trim();
                try {
                    int size = Integer.parseInt(sizeStr);
                    if (size > 23) {
                        columnSize = 22;
                        decimalDigits = 0;
                    } else {
                        columnSize = 12;
                        decimalDigits = 0;
                    }
                } catch (NumberFormatException e) {
                    columnSize = 12;
                    decimalDigits = 0;
                }
            } else {
                columnSize = 12;
                decimalDigits = 0;
            }
        } else if (baseTypeName.equals("DECIMAL") || baseTypeName.equals("NUMERIC")) {
            // Based on lines 268-295 logic
            if (typeInfo.indexOf(",") != -1) {
                String precisionStr = typeInfo.substring(typeInfo.indexOf("(") + 1, typeInfo.indexOf(",")).trim();
                String scaleStr = typeInfo.substring(typeInfo.indexOf(",") + 1, typeInfo.indexOf(")")).trim();
                try {
                    columnSize = Integer.parseInt(precisionStr);
                    decimalDigits = Integer.parseInt(scaleStr);
                } catch (NumberFormatException e) {
                    columnSize = 65;
                    decimalDigits = 0;
                }
            } else {
                columnSize = 65;
                decimalDigits = 0;
            }
        } else if (isStringType(baseTypeName) || isBinaryType(baseTypeName)) {
            // Based on lines 296-340 logic
            if (typeInfo.indexOf("(") != -1) {
                int endParenIndex = typeInfo.indexOf(")");
                if (endParenIndex == -1) {
                    endParenIndex = typeInfo.length();
                }
                
                String sizeStr = typeInfo.substring(typeInfo.indexOf("(") + 1, endParenIndex).trim();
                try {
                    columnSize = Integer.parseInt(sizeStr);
                } catch (NumberFormatException e) {
                    columnSize = getDefaultColumnSize(baseTypeName);
                }
            } else {
                columnSize = getDefaultColumnSize(baseTypeName);
            }
        } else if (isDateTimeType(baseTypeName)) {
            // Based on lines 341-375 logic
            if (baseTypeName.equals("DATE")) {
                columnSize = 10;
            } else if (baseTypeName.equals("TIME")) {
                columnSize = 8;
                if (typeInfo.indexOf("(") != -1) {
                    String fractStr = typeInfo.substring(typeInfo.indexOf("(") + 1, typeInfo.indexOf(")")).trim();
                    try {
                        int fract = Integer.parseInt(fractStr);
                        if (fract > 0) {
                            columnSize += fract + 1;
                        }
                    } catch (NumberFormatException e) {
                        // Keep default value
                    }
                }
            } else if (baseTypeName.equals("DATETIME") || baseTypeName.equals("TIMESTAMP")) {
                columnSize = 19;
                if (typeInfo.indexOf("(") != -1) {
                    String fractStr = typeInfo.substring(typeInfo.indexOf("(") + 1, typeInfo.indexOf(")")).trim();
                    try {
                        int fract = Integer.parseInt(fractStr);
                        if (fract > 0) {
                            columnSize += fract + 1;
                        }
                    } catch (NumberFormatException e) {
                        // Keep default value
                    }
                }
            } else if (baseTypeName.equals("YEAR")) {
                columnSize = 4;
            }
        } else {
            // Other types use default values
            columnSize = getDefaultColumnSize(baseTypeName);
        }
        
        return new TypeInfo(baseTypeName, columnSize, decimalDigits);
    }

    /**
     * Extract base type name - based on existing logic
     */
    private static String extractBaseTypeName(String typeInfo) {
        // Find first space or parenthesis
        int spaceIndex = typeInfo.indexOf(' ');
        int parenIndex = typeInfo.indexOf('(');
        
        int endIndex = typeInfo.length();
        if (spaceIndex != -1 && parenIndex != -1) {
            endIndex = Math.min(spaceIndex, parenIndex);
        } else if (spaceIndex != -1) {
            endIndex = spaceIndex;
        } else if (parenIndex != -1) {
            endIndex = parenIndex;
        }
        
        return typeInfo.substring(0, endIndex).trim();
    }

    /**
     * Check if string type
     */
    private static boolean isStringType(String typeName) {
        return typeName.equals("CHAR") || typeName.equals("VARCHAR") || 
               typeName.equals("TEXT") || typeName.equals("TINYTEXT") ||
               typeName.equals("MEDIUMTEXT") || typeName.equals("LONGTEXT") ||
               typeName.equals("ENUM") || typeName.equals("SET");
    }

    /**
     * Check if binary type
     */
    private static boolean isBinaryType(String typeName) {
        return typeName.equals("BINARY") || typeName.equals("VARBINARY") ||
               typeName.equals("BLOB") || typeName.equals("TINYBLOB") ||
               typeName.equals("MEDIUMBLOB") || typeName.equals("LONGBLOB") ||
               typeName.equals("BIT");
    }

    /**
     * Check if date/time type
     */
    private static boolean isDateTimeType(String typeName) {
        return typeName.equals("DATE") || typeName.equals("TIME") ||
               typeName.equals("DATETIME") || typeName.equals("TIMESTAMP") ||
               typeName.equals("YEAR");
    }

    /**
     * Get default column size - based on DatabaseMetaData default values
     * Based on DatabaseMetaData.TypeDescriptor default value logic
     */
    private static int getDefaultColumnSize(String typeName) {
        if (typeName.equals("TINYINT")) return 3;
        if (typeName.equals("SMALLINT")) return 5;
        if (typeName.equals("MEDIUMINT")) return 7;
        if (typeName.equals("INT") || typeName.equals("INTEGER")) return 10;
        if (typeName.equals("BIGINT")) return 19;
        if (typeName.equals("FLOAT")) return 12;
        if (typeName.equals("DOUBLE")) return 22;
        if (typeName.equals("DECIMAL")) return 65;
        if (typeName.equals("CHAR")) return 1;
        if (typeName.equals("VARCHAR")) return 65535;
        if (typeName.equals("TEXT")) return 65535;
        if (typeName.equals("TINYTEXT")) return 255;
        if (typeName.equals("MEDIUMTEXT")) return 16777215;
        if (typeName.equals("LONGTEXT")) return 2147483647;
        if (typeName.equals("BINARY")) return 255;
        if (typeName.equals("VARBINARY")) return 65535;
        if (typeName.equals("BLOB")) return 65535;
        if (typeName.equals("TINYBLOB")) return 255;
        if (typeName.equals("MEDIUMBLOB")) return 16777215;
        if (typeName.equals("LONGBLOB")) return 2147483647;
        if (typeName.equals("BIT")) return 1;
        if (typeName.equals("JSON")) return 1073741824;
        
        return 255; // Default value
    }

    /**
     * Find matching closing parenthesis - based on existing logic
     */
    private static int findMatchingCloseParen(String str, int openIndex) {
        if (openIndex == -1 || openIndex >= str.length()) {
            return -1;
        }
        
        int count = 1;
        for (int i = openIndex + 1; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '(') {
                count++;
            } else if (c == ')') {
                count--;
                if (count == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
