package org.container.directory;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * This is a simple logger that logs messages and stack traces to the Eclipse
 * error log.
 * 
 * @author Aaron J Tarter
 */
public class Logger {
   public final static String PLUGIN_ID = "org.container.directory";
   // logging severities
   public static final int OK = IStatus.OK;
   public static final int ERROR = IStatus.ERROR;
   public static final int CANCEL = IStatus.CANCEL;
   public static final int INFO = IStatus.INFO;
   public static final int WARNING = IStatus.WARNING;

   // reference to the Eclipse error log
   private static ILog log;

   /**
    * Use the Activator, to get the logger.
    * This ensures, that the logger will never be null.
    */
   private static ILog getLog() {
      if (log == null) {
         log = Activator.getDefault().getLog();
      }
      return log;
   }

   /*
    * Prints stack trace to Eclipse error log
    */
   public static void log(int severity, Throwable e) {
      Status s = new Status(severity, PLUGIN_ID, IStatus.OK, e.getMessage(), e);
      getLog().log(s);
   }

   /*
    * Prints a message to the Eclipse error log
    */
   public static void log(int severity, String msg) {
      Status s = new Status(severity, PLUGIN_ID, IStatus.OK, msg, null);
      getLog().log(s);
   }

}
