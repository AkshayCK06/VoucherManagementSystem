package com.tss.wvms.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WVMS_transMsgInsertProcess {

    // Global variable declarations
    static WVMS_Database conDb;
    static WVMS_generic generic;
    public static String processId;
    static String SignalFlag = "Y";
    static String logTraceFile;
    static String singleLineLog;
    static String moduleName;
    static PreparedStatement transMsgDetPrep;
    static String smsPort;
    static PreparedStatement insertOutSMSPrep;
    static PreparedStatement fetchMsgPrep;
    static PreparedStatement fetchAmtPrep;
    static PreparedStatement updateTransStat;
    static String sendSmsFlag;

    @Value("${wvms.transaction.db.url}")
    private String dbUrl;
    
    @Value("${wvms.transaction.db.username}")
    private String dbUsername;
    
    @Value("${wvms.transaction.db.password}")
    private String dbPassword;
    
    @Value("${wvms.transaction.db.driver}")
    private String dbDriver;

    @Scheduled(fixedDelay = 2000)
    public void transMsgInsertProcess() {
        System.out.println(":::::::::::::::::::[WVMS_transMsgInsertProcess]:::::::::::::::::::::");
        
        SignalFlag = "Y";
        String logDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        logTraceFile = "WVMS_transMsgInsertProcess_" + logDate + ".log";
        singleLineLog = "WVMS_transMsgInsertProcessStatus.log";
        moduleName = "WVMS_transMsgInsertProcess";
        
        // Initialize prepared statements
        transMsgDetPrep = null;
        smsPort = "";
        insertOutSMSPrep = null;
        fetchMsgPrep = null;
        fetchAmtPrep = null;
        updateTransStat = null;
        sendSmsFlag = "";

        while (true) {
            try {
                generic.logfunction(logTraceFile, moduleName + " Connecting to Database*** ");
                conDb = new WVMS_Database(dbUrl, dbUsername, dbPassword, dbDriver);
                conDb.dbConnect();
                generic.logfunction(logTraceFile, " conDb = " + conDb);
                SignalFlag = "N";
                loadQueries();
                generic.logfunction(logTraceFile, " AFTER LOAD QUERIES BEFORE ");
                mainLoop();
            } catch(Exception e) {
                generic.logfunction(logTraceFile, " Exception: " + e.getMessage());
            } finally {
                if (conDb != null) {
                    generic.logfunction(logTraceFile, moduleName + " Disconnecting from Database*** ");
                    closeAllStatements();
                    conDb.dbDisconnect();
                }
            }
            try {
                Thread.sleep(1000);
            } catch(InterruptedException ie) {
                // continue if interrupted
            }
        }
    }

    private void closeAllStatements() {
        try {
            if (transMsgDetPrep != null) transMsgDetPrep.close();
            if (insertOutSMSPrep != null) insertOutSMSPrep.close();
            if (fetchMsgPrep != null) fetchMsgPrep.close();
            if (fetchAmtPrep != null) fetchAmtPrep.close();
            if (updateTransStat != null) updateTransStat.close();
        } catch (SQLException e) {
            generic.logfunction(logTraceFile, "Error closing statements: " + e.getMessage());
        }
    }
    
    public static void mainLoop() {
        generic.logfunction(logTraceFile, " mainLoop started ");
        while (true) {
            try {
                if (SignalFlag.equals("Y")) {
                    generic.logfunction(logTraceFile, moduleName + " *** inside Signal");
                    SignalFlag = "N";
                    loadQueries();
                }

                ResultSet rs = transMsgDetPrep.executeQuery();
                while (rs.next()) {
                    String seqId = rs.getString("SEQ_ID");
                    String msgId = rs.getString("MESSAGE_ID");
                    Long transactionId = rs.getLong("TRANSACTION_ID");
                    String status = rs.getString("STATUS");
                    String date = rs.getString("MSG_DATE");
                    String balDet = rs.getString("BALANCE_DET");
                    String rechargeDet = rs.getString("RECHARGE_DET");
                    
                    generic.logfunction(logTraceFile, "seqId=" + seqId + "|transactionId=" + transactionId + 
                                      "|Date=" + date + "|msgId=" + msgId + "|status=" + status);
                    
                    if (sendSmsFlag.equals("1")) {
                        Object[] msgInfo = getMsg(msgId, transactionId, balDet, rechargeDet);
                        String msgToSend = (String) msgInfo[0];
                        String voucherAmt = (String) msgInfo[1];
                        String subMsisdn = (String) msgInfo[2];
                        
                        int returnVal = insertIntoOutSMS(msgToSend, transactionId, subMsisdn, processId);
                        updateTransactionStatus(returnVal, transactionId);
                    } else {
                        updateTransactionStatus(1, transactionId);
                    }
                }
                rs.close();
                
                Thread.sleep(1000);
            } catch (SQLException | InterruptedException e) {
                generic.logfunction(logTraceFile, "Error in mainLoop: " + e.getMessage());
            }
        }
    }
    
    private static void updateTransactionStatus(int status, long transactionId) throws SQLException {
        updateTransStat.setInt(1, status);
        updateTransStat.setLong(2, transactionId);
        updateTransStat.executeUpdate();
    }
    
    public static Object[] getMsg(String msgId, long transId, String balDet, String rechargeDet) throws SQLException {
        generic.logfunction(logTraceFile, "msgId=" + msgId + "|transId=" + transId + "|balDet=" + balDet);
        
        fetchAmtPrep.setLong(1, transId);
        ResultSet amtRs = fetchAmtPrep.executeQuery();
        if (!amtRs.next()) {
            throw new SQLException("Error fetching amount details");
        }
        
        String voucherAmt = amtRs.getString("VOUCHER_AMOUNT");
        String subMsisdn = amtRs.getString("SUBSCRIBER_MSISDN");
        amtRs.close();
        
        fetchMsgPrep.setString(1, msgId);
        ResultSet msgRs = fetchMsgPrep.executeQuery();
        if (!msgRs.next()) {
            throw new SQLException("Error fetching message");
        }
        
        String msg = msgRs.getString("MESSAGE");
        msgRs.close();
        
        msg = msg.replaceAll("__TRANSID__", String.valueOf(transId))
                 .replaceAll("__DET__", rechargeDet)
                 .replaceAll("__BONUS__", "")
                 .replaceAll("__AMOUNT__", voucherAmt)
                 .replaceAll("__BALDET__", balDet)
                 .replaceAll("__MSISDN__", subMsisdn);
                 
        return new Object[]{msg, voucherAmt, subMsisdn};
    }
    
    public static int insertIntoOutSMS(String msg, long transId, String subMsisdn, String processId) throws SQLException {
        insertOutSMSPrep.setString(1, subMsisdn);
        insertOutSMSPrep.setString(2, msg);
        insertOutSMSPrep.setLong(3, transId);
        insertOutSMSPrep.setString(4, processId);
        return insertOutSMSPrep.executeUpdate();
    }
    
    public static void loadQueries() throws SQLException {
        Connection conn = conDb.getConnection();
        
        String sql = "SELECT SEQ_ID,MESSAGE_ID,TRANSACTION_ID,STATUS,PROCESS_ID,MSG_DATE,BALANCE_DET,RECHARGE_DET " +
                     "FROM WVMS_TRANSACTION_MESSAGES WHERE STATUS=0 and ROWNUM<10";
        transMsgDetPrep = conn.prepareStatement(sql);
        
        smsPort = generic.openConfig("WVMS_SENDSMS_PORT");
        sendSmsFlag = generic.openConfig("WVMS_SEND_MESSAGE_NOTIFICATION_ENABLE");
        
        sql = "INSERT INTO OUT_SMS_Q(MSG_ID,DEST_MSISDN,FROM_MSISDN,MESSAGE,DATE_TIME,MSG_STAT,TRANSACTION_ID,SENDSMS_PORT,OUT_PROCESS_ID) " +
              "values(OUT_SMS_Q_SEQ.nextval,?,?,?,sysdate,'N',?,?,?)";
        insertOutSMSPrep = conn.prepareStatement(sql);
        
        sql = "select MESSAGE from WVMS_MESSAGE_MAST_1 where MESSAGE_ID=?";
        fetchMsgPrep = conn.prepareStatement(sql);
        
        sql = "select VOUCHER_AMOUNT,SUBSCRIBER_MSISDN,RESP_DESC from transaction_mast where TRANSACTION_ID=?";
        fetchAmtPrep = conn.prepareStatement(sql);
        
        sql = "update WVMS_TRANSACTION_MESSAGES set STATUS=? where TRANSACTION_ID=?";
        updateTransStat = conn.prepareStatement(sql);
    }
}

class WVMS_Database {
    private Connection connection;
    private String url;
    private String username;
    private String password;
    private String driver;
    
    public WVMS_Database(String url, String username, String password, String driver) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.driver = driver;
    }
    
    public void dbConnect() throws SQLException, ClassNotFoundException {
        if (connection == null || connection.isClosed()) {
            Class.forName(driver);
            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            connection = DriverManager.getConnection(url, props);
            connection.setAutoCommit(true);
        }
    }
    
    public void dbDisconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
            e.printStackTrace();
            }
            connection = null;
        }
    }
    
    public Connection getConnection() {
        return connection;
    }
}