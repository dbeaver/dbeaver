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

import org.jkiss.dbeaver.ext.db2.editors.DB2ObjectType;

/**
 * DB2 Type of Table Dependency
 * 
 * @author Denis Forveille
 * 
 */
public enum DB2TableDepType {
   A("A (Table alias)", DB2ObjectType.ALIAS),

   F("F (Function)", DB2ObjectType.UDF),

   I("I (Index)", DB2ObjectType.INDEX),

   G("G (Global temporary table)", DB2ObjectType.TABLE),

   N("N (Nickname)"),

   O("O (Privilege dependency on all subtables or subviews in a table or view hierarchy)"),

   R("R (UDT)", DB2ObjectType.UDT),

   S("S (MQT)", DB2ObjectType.TABLE),

   T("T (Table)", DB2ObjectType.TABLE),

   U("U (Typed table)", DB2ObjectType.TABLE),

   V("V (View)", DB2ObjectType.VIEW),

   W("W (Typed view)", DB2ObjectType.VIEW),

   Z("Z (XSR object)"),

   u("u (Module alias)", DB2ObjectType.ALIAS),

   v("v ( Global variable)");

   private String        description;
   private DB2ObjectType db2ObjectType;

   // -----------
   // Constructor
   // -----------

   private DB2TableDepType(String description, DB2ObjectType db2ObjectType) {
      this.description = description;
      this.db2ObjectType = db2ObjectType;
   }

   private DB2TableDepType(String description) {
      this(description, null);
   }

   // ----------------
   // Standard Getters
   // ----------------

   public String getDescription() {
      return description;
   }

   public DB2ObjectType getDb2ObjectType() {
      return db2ObjectType;
   }

}