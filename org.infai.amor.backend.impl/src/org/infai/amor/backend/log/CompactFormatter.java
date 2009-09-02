/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Print log message as one line instead of two like {@link SimpleFormatter}...
 * 
 * @author sdienst
 * 
 */
public class CompactFormatter extends Formatter {

    private static final DateFormat format = new SimpleDateFormat("h:mm:ss");
    private static final String lineSeperator = System.getProperty("line.separator");

    /*
     * (non-Javadoc)
     * 
     * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
     */
    @Override
    public String format(final LogRecord record) {
        String loggerName = record.getLoggerName();
        if (loggerName == null) {
            loggerName = "root";
        }
        final StringBuilder sb = new StringBuilder(loggerName);
        sb.append("[").append(record.getLevel()).append('|');
        sb.append(Thread.currentThread().getName()).append('|');
        sb.append(format.format(new Date(record.getMillis()))).append("]:\t");
        sb.append(record.getMessage()).append(lineSeperator);
        return sb.toString();
    }

}
