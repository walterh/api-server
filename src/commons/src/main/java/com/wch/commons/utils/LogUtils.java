package com.wch.commons.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogUtils {
    public static void logException(String description, Exception e) {
        log.error(description, e);
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