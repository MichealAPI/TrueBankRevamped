package it.mikeslab.truebank.util;

import org.bukkit.Bukkit;

import java.util.logging.Level;

public class LoggerUtil {

    public static void log(Level logLevel, LogSource logSource, Exception exception) {
        Bukkit.getLogger().log(logLevel, "[" + logSource.sourceDisplayName + "]: " + exception.getMessage());
    }

    public static void log(Level logLevel, LogSource logSource, String message) {
        Bukkit.getLogger().log(logLevel, "[" + logSource.sourceDisplayName + "]: " + message);
    }



    public enum LogSource {


        PLUGIN("Plugin"),
        DATABASE("Database"),
        COMMAND("Command"),
        EVENT("Event"),
        CONFIG("Config"),
        UTIL("Util"),
        API("API"),
        OTHER("Other");

        private final String sourceDisplayName;

        LogSource(String sourceDisplayName) {
            this.sourceDisplayName = sourceDisplayName;
        }

    }


}
