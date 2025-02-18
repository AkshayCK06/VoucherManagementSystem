package com.tss.wvms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class WVMS_generic {
    private static final Logger logger = LoggerFactory.getLogger(WVMS_generic.class);
    
    @Value("${wvms.log.directory:/var/log/wvms}")
    private String logDirectory;
    
    @Value("${wvms.sms.port:1234}")
    private String smsPort;
    
    @Value("${wvms.send.message.notification.enable:1}")
    private String sendMessageNotificationEnable;
    
    private final Map<String, String> configCache = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Writes log messages to both file and system logger
     * @param logFile The name of the log file
     * @param message The message to be logged
     */
    public void logfunction(String logFile, String message) {
        String timestamp = dateFormat.format(new Date());
        String formattedMessage = String.format("[%s] %s", timestamp, message);
        
        // Log to system logger
        logger.info(formattedMessage);
        
        // Log to file
        try {
            File logDir = new File(logDirectory);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            File file = new File(logDir, logFile);
            try (FileWriter writer = new FileWriter(file, true)) {
                writer.write(formattedMessage + "\n");
            }
        } catch (IOException e) {
            logger.error("Error writing to log file: " + logFile, e);
        }
    }
    
    /**
     * Retrieves configuration values from Spring properties or cache
     * @param configKey The configuration key to retrieve
     * @return The configuration value
     */
    public String openConfig(String configKey) {
        // Check cache first
        if (configCache.containsKey(configKey)) {
            return configCache.get(configKey);
        }
        
        // If not in cache, get from properties and cache it
        String value = switch (configKey) {
            case "WVMS_SENDSMS_PORT" -> {
                configCache.put(configKey, smsPort);
                yield smsPort;
            }
            case "WVMS_SEND_MESSAGE_NOTIFICATION_ENABLE" -> {
                configCache.put(configKey, sendMessageNotificationEnable);
                yield sendMessageNotificationEnable;
            }
            default -> {
                logger.warn("Unknown configuration key: {}", configKey);
                yield "";
            }
        };
        
        return value;
    }
    
    /**
     * Clears the configuration cache
     */
    public void clearConfigCache() {
        configCache.clear();
    }
    
    /**
     * Formats a log message with timestamp
     * @param message The message to format
     * @return Formatted message with timestamp
     */
    public String formatLogMessage(String message) {
        return String.format("[%s] %s", dateFormat.format(new Date()), message);
    }
    
    /**
     * Checks if a directory exists and creates it if it doesn't
     * @param directoryPath The directory path to check/create
     * @return true if directory exists or was created successfully
     */
    public boolean ensureDirectoryExists(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            return directory.mkdirs();
        }
        return true;
    }
}