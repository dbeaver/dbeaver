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
 * DB2 Type of Trigger Dependency
 * 
 * @author Denis Forveille
 * 
 */
public enum DB2TriggerDepType {
   A("A (Table alias)"),

   B("B (Trigger)"),

   C("C (Column)"),

   F("F (Routine)"),

   G("G (Global temporary table)"),

   H("H (Hierachy table)"),

   K("K (Package)"),

   L("L (Detached table)"),

   N("N (Nickname)"),

   O("O (Privilege dependency on all subtables or subviews in a table or view hierarchy)"),

   Q("Q (Sequence)"),

   R("R (UDT)"),

   S("S (MQT)"),

   T("T (Table)"),

   U("U (Typed table)"),

   V("V (View)"),

   W("W (Typed view)"),

   X("X (Index extension)"),

   Z("Z (XSR object)"),

   q("q (Sequence alias)"),

   u("u (Module alias)"),

   v("v ( Global variable)");

   private String description;

   // -----------
   // Constructor
   // -----------

   private DB2TriggerDepType(String description) {
      this.description = description;
   }

   // ----------------
   // Standard Getters
   // ----------------

   public String getDescription() {
      return description;
   }
}