/*******************************************************************************
 * Copyright (c) 2009 Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at 
 *
 * http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Contributors:
 *
 * Astrient Foundation Inc. 
 * www.astrientfoundation.org
 * rashid@astrientfoundation.org
 * Rashid Mayes 2009
 *******************************************************************************/
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