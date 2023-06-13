package com.rashidmayes.clairvoyance.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
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

    public static String prettyFileName(String fileName) {
        String pattern = "[^a-zA-Z0-9\\.\\-\\_]+";
        return fileName.replaceAll(pattern, "_").trim();
    }

    public static void clearCache() {
        try {
            ClairvoyanceLogger.logger.info("deleting tmp clairvoyance directory");
            var tmpRootDir = new File(System.getProperty("java.io.tmpdir"));
            var clairvoyanceTmpDir = new File(tmpRootDir, "clairvoyance");
            FileUtils.deleteDirectory(clairvoyanceTmpDir);
            ClairvoyanceLogger.logger.info("clairvoyance tmp directory has been deleted");
        } catch (Exception e) {
            ClairvoyanceLogger.logger.error(e.getMessage(), e);
        }
    }

}
