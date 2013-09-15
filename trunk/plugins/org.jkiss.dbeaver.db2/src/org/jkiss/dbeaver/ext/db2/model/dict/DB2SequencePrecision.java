/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.model.dict;

/**
 * DB2 "Precision" of the Sequence
 * 
 * DF: Added a "P" in front of value because Enum don' accept number only values
 * 
 * @author Denis Forveille
 * 
 */
public enum DB2SequencePrecision {
   P5("5 (Smallint)", 5, "SMALLINT"),

   P10("10 (Integer)", 10, "INTEGER"),

   P19("19 (Bigint)", 19, "BIGINT");

   private String  description;
   private Integer dataType;
   private String  sqlKeyword;

   // -----------
   // Constructor
   // -----------
   private DB2SequencePrecision(String description, Integer dataType, String sqlKeyword) {
      this.description = description;
      this.dataType = dataType;
      this.sqlKeyword = sqlKeyword;
   }

   @Override
   public String toString() {
      return description;
   }

   // ------------------------
   // Helpers
   // ------------------------
   public static DB2SequencePrecision getFromDataType(Integer dataType) {
      for (DB2SequencePrecision item : DB2SequencePrecision.values()) {
         if (dataType.equals(item.getDataType())) {
            return item;
         }
      }
      return null;
   }

   // TOOD DF: This to compensante for editor that set the object with the toString value instead of enum itsef. to be removed ASAP
   public static DB2SequencePrecision getFromDescription(String description) {
      for (DB2SequencePrecision item : DB2SequencePrecision.values()) {
         if (description.equals(item.getDescription())) {
            return item;
         }
      }
      return null;
   }

   // ----------------
   // Standard Getters
   // ----------------

   public String getDescription() {
      return description;
   }

   public Integer getDataType() {
      return dataType;
   }

   public String getSqlKeyword() {
      return sqlKeyword;
   }

}