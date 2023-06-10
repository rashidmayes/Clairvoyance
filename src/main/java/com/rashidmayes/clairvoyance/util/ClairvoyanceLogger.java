package com.rashidmayes.clairvoyance.util;

import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class ClairvoyanceLogger {

    public static Logger logger = Logger.getLogger("app");

    // todo: fixme
//    static {
//        try(var stream = ClairvoyanceLogger.class.getResourceAsStream("/logging.properties")) {
//
//            LogManager.getLogManager().readConfiguration(stream);
//            logger = Logger.getLogger(ClairvoyanceLogger.class.getName());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

}
