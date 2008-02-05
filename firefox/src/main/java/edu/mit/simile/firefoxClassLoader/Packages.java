package edu.mit.simile.firefoxClassLoader;


/**
 * Lets Javascript code easily look up a class (wrapped).
 * 
 * @author dfhuynh
 */
public class Packages {
    public ClassWrapper getClass(String name) {
        try {
            Class klass = Class.forName(name);

            if (klass != null) {
                return new ClassWrapper(klass);
            }
        } catch (ClassNotFoundException e) {
            // do nothing, return null
        }
        return null;
    }
}
