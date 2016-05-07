package com.wch.commons.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtils {
    static Logger LOGGER = LoggerFactory.getLogger(LogUtils.class.getName());

    public static void logException(String description, Exception e) {
        LOGGER.error(description, e);
        if (e != null) {
            System.out.println(getStackTrace(e));
        }
    }

    public static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        pw.flush();
        sw.flush();
        
        return sw.toString();
    }

    public static String getExtendedError(Exception ex) {
        String msg = null;

        if (ex != null) {
            msg = ex.getMessage() + "; StackTrace = " + getStackTrace(ex);
        }

        return msg;
    }

    public static Throwable getWrappedException(Exception ex) {
        Throwable t = ex;

        return t.getCause();
    }

    public static Throwable getInnermostException(Exception ex) {
        Throwable t = ex;

        while (t.getCause() != null) {
            t = t.getCause();
        }

        return t;
    }
}