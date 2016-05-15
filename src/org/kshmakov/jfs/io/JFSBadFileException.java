package org.kshmakov.jfs.io;

import org.kshmakov.jfs.driver.JFSException;

public class JFSBadFileException extends JFSException {
    public JFSBadFileException(String message) {
        super(message);
    }
}
