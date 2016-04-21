package org.kshmakov.jfs.driver;

import org.kshmakov.jfs.JFSException;

public class JFSRefuseException extends JFSException {
    public JFSRefuseException(String message) {
        super(message);
    }
}
