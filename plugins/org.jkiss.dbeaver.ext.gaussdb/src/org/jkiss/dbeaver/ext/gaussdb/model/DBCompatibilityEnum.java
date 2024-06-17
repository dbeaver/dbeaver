package org.jkiss.dbeaver.ext.gaussdb.model;

public enum DBCompatibilityEnum {
   ORACLE("Oracle", "A", "ORA"), MYSQL("MySQL", "B", "MYSQL"), TEDATA("Teradata", "C", "TD"), POSTGRES("PostgreSQL", "PG", "PG");

   private final String text;
   private final String cValue;
   private final String dValue;

   private DBCompatibilityEnum(String text, String cValue, String dValue) {
      this.text = text;
      this.cValue = cValue;
      this.dValue = dValue;
   }

   public String getText() {
      return text;
   }

   public String getcValue() {
      return cValue;
   }

   public String getdValue() {
      return dValue;
   }

   public static DBCompatibilityEnum of(String text) {
      DBCompatibilityEnum[] enums = values();
      for (DBCompatibilityEnum e : enums) {
         if (e.text.equals(text)) {
            return e;
         }
      }
      return null;
   }

}