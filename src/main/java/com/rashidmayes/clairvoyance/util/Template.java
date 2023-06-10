package com.rashidmayes.clairvoyance.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.MessageFormat;

public class Template {

    private MessageFormat format;
    private String name;

    private Template(String name, MessageFormat format) {
        this.name = name;
        this.format = format;
    }

    public String getName() {
        return name;
    }

    public String format(Object... args) {
        return format.format(args);
    }

    public static Template getTemplate(String name) throws Exception {
        InputStream is = null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] data = new byte[4 * 1024];

            is = Template.class.getResourceAsStream(name);

            int len;
            while ((len = is.read(data)) != -1) {
                baos.write(data, 0, len);
            }

            MessageFormat format = new MessageFormat(new String(baos.toByteArray()));
            return new Template(name, format);

        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (Exception e) {
            }
        }
    }


    public static String getText(String name) throws Exception {
        InputStream is = null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] data = new byte[4 * 1024];

            is = Template.class.getResourceAsStream(name);

            int len;
            while ((len = is.read(data)) != -1) {
                baos.write(data, 0, len);
            }

            return new String(baos.toByteArray());

        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (Exception e) {
            }
        }
    }
}