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

import org.jkiss.dbeaver.model.struct.DBSObjectState;

/**
 * DB2 Trigger Valid attribute
 * 
 * @author Denis Forveille
 * 
 */
public enum DB2TriggerValid {
   N("N (Invalid)", DBSObjectState.INVALID),

   X("X (Inoperative)", DBSObjectState.INVALID), // TODO DF: No exact correspondance

   Y("Y (Valid)", DBSObjectState.ACTIVE); // TODO DF: No exact correspondance

   private String         description;
   private DBSObjectState state;

   // -----------------
   // Constructor
   // -----------------

   private DB2TriggerValid(String description, DBSObjectState state) {
      this.description = description;
      this.state = state;
   }

   // ----------------
   // Standard Getters
   // ----------------

   public String getDescription() {
      return description;
   }

   public DBSObjectState getState() {
      return state;
   }
}