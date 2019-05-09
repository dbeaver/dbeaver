/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Postgre execution plan node
 */
public class PostgrePlanNodeText extends PostgrePlanNodeBase<PostgrePlanNodeText> {

   private static final String SEPARATOR = " ";
   
   private static final String OPTIONS_SEPARATOR = ":";

   private static final List<String> OPERATIONS = new ArrayList<>(Arrays.asList( "Result", "ProjectSet", "Insert", "Update", "Delete", "Append",
      "Merge Append", "Recursive Union", "BitmapAnd", "BitmapOr", "Nested Loop", "Merge", "Hash", "Hash Join", "Seq Scan",
      "Sample Scan", "Gather", "Gather Merge", "Index Scan", "Index Only Scan", "Bitmap Index Scan",
      "Bitmap Heap Scan", "Tid Scan", "Subquery Scan", "Function Scan", "Table Function Scan", "Values Scan",
      "CTE Scan", "Named Tuplestore Scan", "WorkTable Scan", "Foreign Scan", "Foreign Insert", "Foreign Update",
      "Foreign Delete", "???", "Custom Scan", "Materialize", "Sort", "Group", "Aggregate", "GroupAggregate",
      "HashAggregate", "MixedAggregate", "Aggregate ???", "Partial", "Finalize", "Simple", "WindowAgg", "Unique",
      "SetOp", "HashSetOp", "SetOp ???", "LockRows", "Limit", "Hash"));
    
    private static final Set<String> OPERATION_TABLES = new HashSet<String>(Arrays.asList("Insert", "Update", "Delete", "Seq Scan",  "Foreign Scan", "Foreign Insert", "Foreign Update"));
   
    private static final Set<String> OPERATION_INDEXES = new HashSet<String>(Arrays.asList("Index Scan", "Index Only Scan"));
   
    private static final Set<String> OPERATION_FUNCTION = new HashSet<String>(Arrays.asList("Subquery Scan", "Function Scan"));
   
    private int infoSeq = 1;
    
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

        for (int index = getTokenIndex(tokens, start, marker); index < tokens.length; index++) {
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
        
        String operation = OPERATIONS.stream().filter(op -> str.startsWith(op)).max(Comparator.comparing(String::length)).orElse("N/A");
        
        String tokens[] = str.substring(operation.length()).split(SEPARATOR);
 
        if (OPERATION_TABLES.contains(operation)) {
               
           attributes.put(PostgrePlanNodeBase.ATTR_RELATION_NAME, getTokenAfter(tokens, 1, "on"));
           
        } else if (OPERATION_INDEXES.contains(operation)) {
            
            attributes.put(PostgrePlanNodeBase.ATTR_INDEX_NAME, getTokenAfter(tokens, 1, "on"));
            
        } else if (OPERATION_FUNCTION.contains(operation)) {
            
            attributes.put(PostgrePlanNodeBase.ATTR_FUNCTION_NAME, getTokenAfter(tokens, 1, "on"));
            
        }

        attributes.put(PostgrePlanNodeBase.ATTR_NODE_TYPE, operation);

        parseAttr(attributes, tokens);
        
        setAttributes(attributes);
        
        if (parent != null) {
            parent.nested.add(this);
        }
    }

}
