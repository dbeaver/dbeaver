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
package org.jkiss.dbeaver.ext.db2.model.dict;

import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;

/**
 * DB2 Foreign Key Rule
 * 
 * @author Denis Forveille
 * 
 */
public enum DB2DeleteUpdateRule {
   A("A (No Action)", DBSForeignKeyModifyRule.NO_ACTION),

   C("C (Cascade)", DBSForeignKeyModifyRule.CASCADE),

   N("F (Set Null)", DBSForeignKeyModifyRule.SET_NULL),

   R("R (Restrict)", DBSForeignKeyModifyRule.RESTRICT);

   private String                  description;
   private DBSForeignKeyModifyRule rule;

   // ------------
   // Constructors
   // ------------
   private DB2DeleteUpdateRule(String description, DBSForeignKeyModifyRule rule) {
      this.description = description;
      this.rule = rule;
   }

   // ----------------
   // Standard Getters
   // ----------------

   public String getDescription() {
      return description;
   }

   public DBSForeignKeyModifyRule getRule() {
      return rule;
   }
}