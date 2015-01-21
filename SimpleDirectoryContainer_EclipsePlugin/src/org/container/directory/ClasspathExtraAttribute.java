package org.container.directory;

/**
 * Enums for the additional attributes.
 * @author Darko Palic
 */
public enum ClasspathExtraAttribute {
   FILE_EXTENSTIONS("extensions");

   private String value;
   private ClasspathExtraAttribute(String pValue) {
      value = pValue;
   }

   public String getValue() {
      return value;
   }
}
