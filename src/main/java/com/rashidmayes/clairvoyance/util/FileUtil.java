package com.rashidmayes.clairvoyance.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class FileUtil {

    public static final double KIB = Double.valueOf(Math.pow(2, 10)).intValue();
    public static final double MIB = Double.valueOf(Math.pow(2, 20)).intValue();
    public static final double GIB = Double.valueOf(Math.pow(2, 30)).intValue();

    public static String getSizeString(long number, Locale locale) {
        String suffix;
        double sz;

        if (number == 0) {
            return "0";
        } else if (number >= GIB) {
            //gb
            sz = (number / GIB);
            suffix = "GB";
        } else if (number >= MIB) {
            //mb
            sz = (number / MIB);
            suffix = "MB";
        } else if (number >= KIB) {
            //kb
            sz = number / KIB;
            suffix = "KB";
        } else {
            sz = number;
            suffix = "B";
        }


        NumberFormat nf = NumberFormat.getNumberInstance(locale);
        nf.setMaximumFractionDigits(2);

        return nf.format(sz) + suffix;
    }

    public static String prettyFileName(String fileName, String ext, boolean spaces) {
        String pattern = (spaces) ? "[^a-zA-Z0-9\\.\\-\\_\\ ]+" : "[^a-zA-Z0-9\\.\\-\\_]+";
        String name = fileName.replaceAll(pattern, "_").trim();
        if (ext != null) {
            name = name + "." + ext;
        }

        return name;
    }
}
