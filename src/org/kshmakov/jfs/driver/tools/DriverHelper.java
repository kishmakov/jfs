package org.kshmakov.jfs.driver.tools;

import org.kshmakov.jfs.driver.JFSRefuseException;

public interface DriverHelper {
    static void refuseIf(boolean condition, String message) throws JFSRefuseException {
        if (condition) {
            throw new JFSRefuseException(message);
        }
    }
}
