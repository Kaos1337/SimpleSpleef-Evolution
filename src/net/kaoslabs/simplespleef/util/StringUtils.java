package net.kaoslabs.simplespleef.util;

import java.util.List;

public class StringUtils {

	/**
    *
    * @param list The List to be represented as a String
    * @param header The String prepended to the String representation
    * @param separator The String separating each List element
    * @param footer The String appended to the String representation
    *
    * @return The String representation of the List
    */
   public static String listToString(List<?extends Object> list, String header, String separator, String footer) {
       String delim = "";
       StringBuilder sb = new StringBuilder(header);

       for (int i = 0; i < list.size(); i++) {
           sb.append(delim).append("" + list.get(i));
           delim = separator;
       }

       return sb.append(footer).toString();
   }
	
}
