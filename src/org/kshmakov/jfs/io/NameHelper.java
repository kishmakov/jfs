package org.kshmakov.jfs.io;

import org.kshmakov.jfs.JFSException;

import java.io.UnsupportedEncodingException;

public interface NameHelper {
    char SEPARATOR = '/';
    String CHARSET = "UTF-8";
    short MAX_NAME_SIZE = 255;

    static String inspect(String name) throws JFSException {
        if (name.isEmpty()) {
            return "entry name must not be empty";
        }

        if (name.indexOf(SEPARATOR) != -1) {
            return "entry name must not contain separator character";
        }

        try {
            if (name.getBytes(CHARSET).length > MAX_NAME_SIZE) {
                return "entry name length limit exceeded";
            }
        } catch (UnsupportedEncodingException e) {
            throw new JFSException("jfs requires " + CHARSET + " support to be turned on");
        }

        return "";
    }

    static byte[] toBytes(String name) throws JFSException {
        assert inspect(name).isEmpty();

        try {
            return name.getBytes(NameHelper.CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new JFSException("jfs requires " + CHARSET + " support to be turned on");
        }
    }

    static String fromBytes(byte[] bytes) throws JFSException {
        try {
            return new String(bytes, NameHelper.CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new JFSException("jfs requires " + CHARSET + " support to be turned on");
        }
    }
}
