package com.tss.wvms.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;

public class WVMS_Outprocess {
    private static final Logger logger = Logger.getLogger(WVMS_Outprocess.class.getName());
    private static String myProgram = "WVMS_OutProcess.java";
    private static int processId;
    private static Connection conDb = null;
    private static WVMS_Generic generic;
    private static String Signal = "Y";
    
    private static PreparedStatement SelOutSmsQPre;
    private static PreparedStatement DelOutSmsQPre;
    private static PreparedStatement updateOutSmsQPre;
    private static PreparedStatement updateOutSmsQPreN;
    private static PreparedStatement failedMsgPrep;
    private static String ipAddress;
    private static String ccCode;
    private static String SENDSMSURL;
    
    private static int retryConfig;
    private static int WVMS_SLEEP_TIME;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("No Valid Process Id specified, killing the program and exiting");
            System.exit(0);
        }

        try {
            processId = Integer.parseInt(args[0].trim());
            if (processId == 0) {
                System.out.println("No Valid Process Id specified, killing the program and exiting");
                System.exit(0);
            }
        } catch (NumberFormatException e) {
            System.out.println("No Valid Process Id specified, killing the program and exiting");
            System.exit(0);
        }

        // Check if the process is already running
        // Note: Implementing a similar check in Java is non-trivial and may require OS-specific commands.
        // For simplicity, this check is omitted.

        Runtime.getRuntime().addShutdownHook(new Thread(() -> closePrg("SHUTDOWN")));

        generic = new WVMS_Generic();

        while (true) {
            try {
                writeDBLog(" In Eval Connecting to Database");
                conDb = WVMS_PerfDatabase.dbCon();
                ConfigurationLoad(1);
                Signal = "N";
                mainProcess();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception occurred: ", e);
            } finally {
                writeDBLog(" Disconnecting from Database");
                closePreparedStatements();
                WVMS_PerfDatabase.dbDiscon(conDb);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                logger.log(Level.WARNING, "Sleep interrupted", ie);
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void mainProcess() {
        while (true) {
            try {
                if (Signal.equals("Y")) {
                    generic.tracesForAll(myProgram + " *** inside Signal");
                    Signal = "N";
                    ConfigurationLoad();
                }

                ResultSet res = SelOutSmsQPre.executeQuery();
                if (res == null) {
                    generic.tracesForAll("Query Failed " + SelOutSmsQPre.toString() + "- Failed");
                    WVMS_PerfDatabase.dbDiscon(conDb);
                    throw new SQLException("Query execution failed.");
                }

                while (res.next()) {
                    int MsgId = res.getInt("MSG_ID");
                    String To = res.getString("DEST_MSISDN");
                    String From = res.getString("FROM_MSISDN");
                    String Message = res.getString("MESSAGE");
                    String Udh = res.getString("UDH");
                    String SendSmsPort = res.getString("SENDSMS_PORT");
                    int unicode = res.getInt("OPTIONAL_FLAG");
                    int retryCnt = res.getInt("RETRY_COUNT");

                    // Updating MSG_STAT to P
                    updateOutSmsQPre.setInt(1, MsgId);
                    if (updateOutSmsQPre.executeUpdate() == 0) {
                        generic.tracesForAll("Query Failed " + updateOutSmsQPre.toString() + "- Failed");
                        WVMS_PerfDatabase.dbDiscon(conDb);
                        throw new SQLException("Update execution failed.");
                    }

                    From = From.trim().replaceAll("\\s+", " ").replace("+", "");
                    To = To.trim().replaceAll("\\s+", " ").replace("+", "");
                    To = ccCode + To;
                    if (SendSmsPort == null || SendSmsPort.isEmpty()) {
                        SendSmsPort = From.replaceAll("\\s", "");
                    }

                    String queryString;
                    if (Udh != null && !Udh.isEmpty()) {
                        Message = Hex2Char(Message);
                        Message = URLEncoder.encode(Message, StandardCharsets.UTF_8);
                        Udh = Hex2Char(Udh);
                        Udh = URLEncoder.encode(Udh, StandardCharsets.UTF_8);
                        queryString = SENDSMSURL + ":" + SendSmsPort + "/cgi-bin/sendsms?user=tester&pass=foobar&text=" + Message + "&to=" + To + "&udh=" + Udh;
                    } else {
                        Message = Message.trim();
                        Message = URLEncoder.encode(Message, StandardCharsets.UTF_8);
                        if (unicode == 0) {
                            queryString = SENDSMSURL + ":" + SendSmsPort + "/cgi-bin/sendsms?user=tester&pass=foobar&text=" + Message + "&to=" + To;
                        } else {
                            queryString = SENDSMSURL + ":" + SendSmsPort + "/cgi-bin/sendsms?user=tester&pass=foobar&text=" + Message + "&to=" + To + "&coding=2";
                        }
                    }

                    writeOutLog("QueryString :: " + queryString);

                    int SendToMobile = getURLContent(queryString);
                    writeOutLog("Response SendToMobile =" + SendToMobile);
                    if (SendToMobile == 1) {
                        DelOutSmsQPre.setInt(1, MsgId);
                        if (DelOutSmsQPre.executeUpdate() == 0) {
                            generic.tracesForAll("Query Failed " + DelOutSmsQPre.toString() + "- Failed");
                            WVMS_PerfDatabase.dbDiscon(conDb);
                            throw new SQLException("Delete execution failed.");
                        }
                    } else {
                        writeOutLog("retryCnt  :: " + retryCnt + " >= " + retryConfig + " :: retryConfig");
                        if (retryCnt >= retryConfig) {
                            writeOutLog("Message sending failed after configured retry for msg id ::" + MsgId);
                            failedMsgPrep.setInt(1, MsgId);
                            if (failedMsgPrep.executeUpdate() == 0) {
                                generic.tracesForAll("Query Failed " + failedMsgPrep.toString() + "- Failed");
                                WVMS_PerfDatabase.dbDiscon(conDb);
                                throw new SQLException("Failed message update execution failed.");
                            }
                        } else {
                            writeOutLog("Updating Message with msg id ::" + MsgId + " as message sending has failed ");
                            updateOutSmsQPreN.setInt(1, MsgId);
                            if (updateOutSmsQPreN.executeUpdate() == 0) {
                                generic.tracesForAll("Query Failed " + updateOutSmsQPreN.toString() + "- Failed");
                                WVMS_PerfDatabase.dbDiscon(conDb);
                                throw new SQLException("Retry update execution failed.");
                            }
                        }
                    }
                }
                res.close();
                Thread.sleep(WVMS_SLEEP_TIME * 1000L);
            } catch (SQLException | InterruptedException e) {
                logger.log(Level.SEVERE, "Exception in mainProcess: ", e);
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void closePrg(String sig) {
        try {
            closePreparedStatements();
            WVMS_PerfDatabase.dbDiscon(conDb);
            writeLog("Caught Signal " + sig);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception in closePrg: ", e);
        }
        System.exit(0);
    }

    private static void ConfigurationLoad() throws SQLException {
        ConfigurationLoad(0);
    }

    private static void ConfigurationLoad(int from) throws SQLException {
        if (conDb != null && from != 1) {
            WVMS_PerfDatabase.dbDiscon(conDb);
            conDb = WVMS_PerfDatabase.dbCon();
        }

        // Read Configs
        WVMS_SLEEP_TIME = Integer.parseInt(generic.openConfig("WVMS_SLEEP_TIME"));
        retryConfig = Integer.parseInt(generic.openConfig("WVMS_MSG_RETY_CNT"));

        String SelOutSmsQ = "SELECT MSG_ID, DEST_MSISDN, FROM_MSISDN, MESSAGE, UDH, SENDSMS_PORT, OPTIONAL_FLAG, RETRY_COUNT FROM OUT_SMS_Q WHERE MSG_STAT='N' AND DATE_TIME < SYSDATE AND OUT_PROCESS_ID = ? AND ROWNUM < 30 ORDER BY MSG_ID";
        SelOutSmsQPre = conDb.prepareStatement(SelOutSmsQ);
        SelOutSmsQPre.setInt(1, processId);

        String DelOutSmsQ = "UPDATE OUT_SMS_Q SET MSG_STAT='Y' WHERE MSG_ID = ? AND MSG_STAT='P'";
        DelOutSmsQPre = conDb.prepareStatement(DelOutSmsQ);

        String updateOutSmsQ = "UPDATE OUT_SMS_Q SET MSG_STAT='P' WHERE MSG_ID = ? AND MSG_STAT='N'";
        updateOutSmsQPre = conDb.prepareStatement(updateOutSmsQ);

        String updateOutSmsQN = "UPDATE OUT_SMS_Q SET MSG_STAT='N', DATE_TIME = (SYSDATE + (?/86400)), RETRY_COUNT=RETRY_COUNT+1 WHERE MSG_ID = ? AND MSG_STAT='P'";
        updateOutSmsQPreN = conDb.prepareStatement(updateOutSmsQN);
        updateOutSmsQPreN.setInt(1, WVMS_SLEEP_TIME);

        String failedMsg = "UPDATE OUT_SMS_Q SET MSG_STAT='F' WHERE MSG_ID = ?";
        failedMsgPrep = conDb.prepareStatement(failedMsg);

        String sql = "SELECT config_value FROM VMS_CONFIGFILE_MAST WHERE config_name = 'WVMS_KANNEL_IP' AND ROWNUM = 1";
        Statement stmt = conDb.createStatement();
        ResultSet res = stmt.executeQuery(sql);
        if (res.next()) {
            ipAddress = res.getString("config_value");
        }
        res.close();
        stmt.close();

        ccCode = generic.openConfig("WVMS_COUNTRY_CODE");
        SENDSMSURL = generic.openConfig("WVMS_SENDSMS_URL");
    }

    private static int getURLContent(String url) {
        try {
            java.net.URL urlObj = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder buffer = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    buffer.append(inputLine).append("\n");
                }
                in.close();
                String response = buffer.toString();
                response = response.replaceAll("^(.*?)\\n\\n(.*?)$", "$2");
                if (response.contains("0: Accepted for delivery")) {
                    writeLog("Success - " + url + " \t " + response);
                    return 1;
                } else {
                    writeFailureLog("FAILURE DIFFERENT STATE \t " + url + " \t " + response);
                    writeHeartLog("FAILURE DIFFERENT STATE \t " + url + " \t " + response);
                    return 0;
                }
            } else {
                writeFailureLog("FAILURE - ERROR in connecting to Server - " + url);
                writeHeartLog("FAILURE -ERROR in connecting to Server - " + url);
                return 0;
            }
        } catch (IOException e) {
            writeFailureLog("FAILURE - ERROR in connecting to Server - " + url);
            writeHeartLog("FAILURE -ERROR in connecting to Server - " + url);
            return 0;
        }
    }

    private static String Hex2Char(String inString) {
        StringBuilder outString = new StringBuilder();
        for (int i = 0; i < inString.length(); i += 2) {
            String hexChar = inString.substring(i, i + 2);
            outString.append((char) Integer.parseInt(hexChar, 16));
        }
        return outString.toString();
    }

    private static void writeLog(String msg) {
        String now = getCurrentTimestamp();
        String logDate = getCurrentLogDate();
        String file = System.getenv("VMS_HOME") + "/" + System.getenv("VMS_LOG_PATH") + "/" + logDate + "WVMS_OutSmsQSuccess.log";
        appendToFile(file, now + "\t" + processId + "\t" + msg);
    }

    private static void writeHeartLog(String msg) {
        String now = getCurrentTimestamp();
        String logDate = getCurrentLogDate();
        String file1 = System.getenv("VMS_HOME") + "/" + System.getenv("VMS_LOG_PATH") + "/" + logDate + "WVMS_issueWithKannel.log";
        String file2 = System.getenv("VMS_HOME") + "/" + System.getenv("VMS_LOG_PATH") + "/" + logDate + "WVMS_issueWithKannelBacked.log";
        appendToFile(file1, now + "\t" + processId + "\t" + msg);
        appendToFile(file2, now + "\t" + processId + "\t" + msg);
    }

    private static void writeFailureLog(String msg) {
        String now = getCurrentTimestamp();
        String logDate = getCurrentLogDate();
        String file = System.getenv("VMS_HOME") + "/" + System.getenv("VMS_LOG_PATH") + "/" + logDate + "WVMS_OutSmsQFailure.log";
        appendToFile(file, now + "\t" + processId + "\t" + msg);
    }

    private static void writeDBLog(String msg) {
        String now = getCurrentTimestamp();
        String logDate = getCurrentLogDate();
        String fileName = System.getenv("VMS_HOME") + "/" + System.getenv("VMS_LOG_PATH") + "/" + logDate + "WVMS_OutProcessDb.log";
        appendToFile(fileName, now + " => " + msg);
    }

    private static void writeOutLog(String msg) {
        String now = getCurrentTimestamp();
        String logDate = getCurrentLogDate();
        String fileName = System.getenv("VMS_HOME") + "/" + System.getenv("VMS_LOG_PATH") + "/" + logDate + "WVMS_OutProcess.log";
        appendToFile(fileName, now + " => " + msg);
    }

    private static String getCurrentTimestamp() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return LocalDateTime.now().format(dtf);
    }

    private static String getCurrentLogDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        return sdf.format(new java.util.Date());
    }

    private static void appendToFile(String filePath, String text) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, true))) {
            bw.write(text);
            bw.newLine();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to write to log file: " + filePath, e);
        }
    }

    private static void closePreparedStatements() {
        try {
            if (SelOutSmsQPre != null && !SelOutSmsQPre.isClosed()) {
                SelOutSmsQPre.close();
            }
            if (DelOutSmsQPre != null && !DelOutSmsQPre.isClosed()) {
                DelOutSmsQPre.close();
            }
            if (updateOutSmsQPre != null && !updateOutSmsQPre.isClosed()) {
                updateOutSmsQPre.close();
            }
            if (updateOutSmsQPreN != null && !updateOutSmsQPreN.isClosed()) {
                updateOutSmsQPreN.close();
            }
            if (failedMsgPrep != null && !failedMsgPrep.isClosed()) {
                failedMsgPrep.close();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error closing prepared statements: ", e);
        }
    }
}

class WVMS_PerfDatabase {
    @Value("spring.datasource.url")
    private static String DB_URL;
    @Value("spring.datasource.username")
    private static String USER;
    @Value("spring.datasource.password")
    private static String PASS;

    public static Connection dbCon() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    public static void dbDiscon(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                Logger.getLogger(WVMS_PerfDatabase.class.getName()).log(Level.SEVERE, "Error closing DB connection", e);
            }
        }
    }
}

class WVMS_Generic {
    private static final Logger logger = Logger.getLogger(WVMS_Generic.class.getName());

    public String openConfig(String configName) {
       
        Properties props = new Properties();
        try {
            props.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
            return props.getProperty(configName);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load configuration for " + configName, e);
            return "";
        }
    }

    public void tracesForAll(String message) {
        logger.info(message);
    }
}
