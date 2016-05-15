package org.kshmakov.jfs.driver.tools;

import org.kshmakov.jfs.driver.JFSException;

public interface DriverHelper {
    static void refuseIf(boolean condition, String message) throws JFSException {
        if (condition) {
            throw new JFSException(message);
        }
    }
}
