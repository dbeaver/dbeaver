package org.jkiss.dbeaver.ext.gaussdb.ui.editors;

import org.jkiss.dbeaver.ext.postgresql.ui.editors.PostgreSourceViewEditor;

public class GaussDBPackageBodyViewEditor extends PostgreSourceViewEditor {
   @Override
   protected boolean isReadOnly() {
      return false;
   }
}