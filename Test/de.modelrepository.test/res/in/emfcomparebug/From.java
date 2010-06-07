package org.apache.commons.lang;

public class ObjectUtils {

    public static boolean equals(Object object1, Object object2) {
        if (object1 == null) {
            return (object2 == null);
        } else if (object2 == null) {
            return false;
        } else {
            return object1.equals(object2);
        }
    }
    
}
