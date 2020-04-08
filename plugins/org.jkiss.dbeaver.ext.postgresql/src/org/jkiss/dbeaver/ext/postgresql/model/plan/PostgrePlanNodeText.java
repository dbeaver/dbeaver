/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model.plan;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;

import java.util.*;


/**
 * Postgre execution plan node
 */
public class PostgrePlanNodeText extends PostgrePlanNodeBase<PostgrePlanNodeText> {
    
    private static final int OPERATION_LEN_MIN = 2;

    private static final String SEPARATOR = " ";
   
    private static final String OPTIONS_SEPARATOR = ":";

    private static final Set<String> OPERATION_TABLES = new HashSet<String>(Arrays.asList("Insert", "Update", "Delete", "Seq",  "Foreign"));
   
    private static final Set<String> OPERATION_INDEXES = new HashSet<String>(Arrays.asList("Index"));
   
    private static final Set<String> OPERATION_FUNCTION = new HashSet<String>(Arrays.asList("Subquery", "Function"));
   
    public static final String ATTR_ADD_NAME = "Info";
    
    private int infoSeq = 0;
    
    private int indent;
    
    public int getIndent() {
        return indent;
    }

    private int getTokenIndex(String tokens[], int start, String marker) {
        return getTokenIndex(tokens, start, marker, false);
    }

    private int getTokenIndex(String tokens[], int start, String marker, boolean caseSensetive) {

        if (start < 0 || start >= tokens.length) {
            return -1;
        }

        for (int index = start; index < tokens.length; index++) {

            if (caseSensetive) {
                if (tokens[index].equals(marker)) {
                    return index;
                }
            } else {
                if (tokens[index].equalsIgnoreCase(marker)) {
                    return index;
                }
            }

        }

        return -1;

    }

    private String getTokenAfter(String tokens[], int start, String marker) {
        
        int tokenIndex = getTokenIndex(tokens, start, marker);
        
        if (tokenIndex < 0) {
            return null;
        }
        
        for (int index = tokenIndex; index < tokens.length; index++) {
            if (tokens[index].length() == 0 || tokens[index] == SEPARATOR) {
                continue;
            }

            return ((index + 1) < tokens.length) ? tokens[index+1] : "";

        }

        return "";
    }

    
    private String removePrefix(String value) {
        int firstChar = 0;
        
        for(int index =0;index < value.length();index++) {
            if(Character.isLetter(value.charAt(index))) {
                firstChar = index;
                break;
            }
        }
        
        if (firstChar == (value.length() -1) ) {
            return "";
        }
        
        return value.substring(firstChar);
    }
    
    private String[] splitPair(String value) {
        String[] result = new String[] {"",""};
        
        if (value == null) {
            return result;
        }
        
        String split[] = value.split("\\.\\.");
        
        if (split.length == 0) {
            return result;
        } else  if (split.length == 1) {
            result[0] = split[0];
            result[1] = null;
        } else {
            result[0] = split[0];
            result[1] = split[1];
        }
        
        return result;
    }
    
    private String getAdditional(String tokens[]) {
        
        StringBuilder sb = new StringBuilder();
        boolean isObjectName = false;
        
        for(int index = 1; index < tokens.length;index++) {
            if (tokens[index].equals(SEPARATOR)) {
                continue;
            }
            if (tokens[index].startsWith("(")) {
                break;
            }
            if (isObjectName) {
                isObjectName = false;
                continue;
            }
            if (tokens[index].equalsIgnoreCase("on")) {
                isObjectName = true;
                continue;
            }
            if (sb.length() > 0) {
                sb.append(SEPARATOR);
            }
                sb.append(tokens[index]);
            
        }
        
        return sb.toString().trim();
    }
    
    private void addAttr(Map<String, String> attributes,String attrName1,String attrName2, String attrVal) {
        String[] pair = splitPair(attrVal);
        attributes.put(attrName1,pair[0]);
        attributes.put(attrName2,pair[1]);
    }
    
    private void addAttr(Map<String, String> attributes,String attrName, String attrVal) {
        
        attrName = attrName.startsWith("(") ? attrName.substring(1) : attrName;
        attrVal = attrVal.endsWith(")") ? attrVal.substring(0,attrVal.length()-1) : attrVal;
        
        switch (attrName) {
        case "cost":
            addAttr(attributes,PostgrePlanNodeBase.ATTR_STARTUP_COST,PostgrePlanNodeBase.ATTR_TOTAL_COST,attrVal);
            break;
        case "rows":
            addAttr(attributes,PostgrePlanNodeBase.ATTR_PLAN_ROWS,PostgrePlanNodeBase.ATTR_ACTUAL_ROWS,attrVal);
            break;

        default:
            attributes.put(attrName,attrVal);
        }
        
    }
    
    private void parseAttr(Map<String, String> attributes, String tokens[]) {
        for(String token : tokens) {
            int posSep = token.indexOf('=');
            if (posSep > 0) {                
                addAttr(attributes,token.substring(0, posSep),token.substring(posSep+1));
            }
        }
    }
    
    public void addProp(String line) {
        
        
        int optIdx = line.indexOf(OPTIONS_SEPARATOR);
        
        if (optIdx == -1) {
            
            this.attributes.put("Info " + (++infoSeq), line);
            
        } else {
            
            this.attributes.put( line.substring(0,optIdx).trim(),line.substring(optIdx+1,line.length()).trim());
            
        }
        
    }
    
    public PostgrePlanNodeText(PostgreDataSource dataSource, PostgrePlanNodeText parent, String line, int indent) {
        super(dataSource, parent);
        
        this.indent = indent;
        
        Map<String, String> attributes = new LinkedHashMap<>();
        
        String str = removePrefix(line);
        
        String tokens[] = str.split(SEPARATOR);
        
        if (tokens.length > 0) {
            
            String operation = tokens[0];
            
            parseObjName(attributes, tokens, operation);
            
            String addInfo = getAdditional(tokens);
            
            if (operation.length() >= OPERATION_LEN_MIN && addInfo.length() > 0) {
                
                attributes.put(PostgrePlanNodeBase.ATTR_NODE_TYPE, addInfo); 
                
            } else {
            
                attributes.put(PostgrePlanNodeBase.ATTR_NODE_TYPE, operation);
                
                if (addInfo.length() > 0) {
                    
                    attributes.put(ATTR_ADD_NAME, addInfo);
                    
                }
            
            }

            parseAttr(attributes, tokens); 
        }
        
        setAttributes(attributes);
        
        if (parent != null) {
            parent.nested.add(this);
        }
    }

    private void parseObjName(Map<String, String> attributes, String[] tokens, String operation) {
        
        String objName = getTokenAfter(tokens, 1, "on");
        
        if (objName != null) {
 
            if (OPERATION_TABLES.contains(operation)) {
                   
               attributes.put(PostgrePlanNodeBase.ATTR_RELATION_NAME, objName);
               
            } else if (OPERATION_INDEXES.contains(operation)) {
                
                attributes.put(PostgrePlanNodeBase.ATTR_INDEX_NAME, objName);
                
            } else if (OPERATION_FUNCTION.contains(operation)) {
                
                attributes.put(PostgrePlanNodeBase.ATTR_FUNCTION_NAME, objName);
                
           } else {
               
               attributes.put(PostgrePlanNodeBase.ATTR_OBJECT_NAME, objName);
               
           }
            
        }
    }

}
