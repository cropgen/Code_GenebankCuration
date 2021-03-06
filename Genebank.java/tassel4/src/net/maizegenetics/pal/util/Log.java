// Log.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)

package net.maizegenetics.pal.util;

/**
 * Log provides a mechanism for logging and debugging to the
 * standard output stream.
 *
 * @author Alexei Drummond
 * @version $Revision: 1.1 $
 */
public class Log implements Logger {

    static Logger defaultLogger = new Log();

    private boolean isDebugOn = false;
    
    public void setDebug(boolean on) {
	
        isDebugOn = on;
    }

    public boolean isDebugging() {
	return isDebugOn;
    }
    
    public void log(Object s) {
	System.out.println(s);
    }

    public void debug(Object s) {

        if (isDebugOn) {
	    log(s);
        }
    }

    public static Logger getDefaultLogger() {
	return defaultLogger;
    }

    public static void setDefaultLogger(Logger logger) {
	defaultLogger = logger;
    }

}



