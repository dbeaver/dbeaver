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
 */package org.jkiss.dbeaver.ext.db2.edit;

import org.jkiss.dbeaver.ext.db2.model.DB2Sequence;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2SequencePrecision;
import org.jkiss.dbeaver.ui.properties.IPropertyValueListProvider;

/**
 * Groups all List providers for enums used by editors
 * 
 * @author Denis Forveille
 * 
 */
public class DB2EnumListProvideHelper {

   public static class DB2SequencePrecisionListProvider implements IPropertyValueListProvider<DB2Sequence> {

      @Override
      public boolean allowCustomValue() {
         return false;
      }

      @Override
      public Object[] getPossibleValues(DB2Sequence sequence) {
         return DB2SequencePrecision.values();
      }
   }

   // --------------
   // Constructor
   // --------------
   private DB2EnumListProvideHelper() {
      // Pure utility class
   }

}
