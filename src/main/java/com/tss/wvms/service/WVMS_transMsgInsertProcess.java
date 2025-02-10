package com.tss.wvms.service;



import java.io.BufferedReader;

import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sun.misc.Signal;
import sun.misc.SignalHandler;


public class WVMS_transMsgInsertProcess {

    // Global variable declarations
    static WVMS_perlDatabase conDb;
    static WVMS_generic generic;
    static String processId;
    static String SignalFlag = "Y";
    static String logTraceFile;
    static String singleLineLog;
    static String moduleName;
    static QueryPrepared TransMsgDetPrep;
    static String smsPort;
    static QueryPrepared insertOutSMSPrep;
    static QueryPrepared fetchMsgPrep;
    static QueryPrepared fetchAmtPrep;
    static QueryPrepared updateTransStat;
    static String sendSmsFlag;
    
    public static void main(String[] args) {
       
        // Register signal handlers using sun.misc.Signal
        Signal.handle(new Signal("TERM"), new SignalHandler() {
            public void handle(Signal sig) {
                closePrg();
            }
        });
        Signal.handle(new Signal("INT"), new SignalHandler() {
            public void handle(Signal sig) {
                closePrg();
            }
        });
        Signal.handle(new Signal("HUP"), new SignalHandler() {
            public void handle(Signal sig) {
                // Mimic: sub { die; };
                System.exit(1);
            }
        });
        Signal.handle(new Signal("USR1"), new SignalHandler() {
            public void handle(Signal sig) {
                handleSignal();
            }
        });

        // Initialize generic and process id handling
        generic = new WVMS_generic();
        if (args.length < 1) {
            System.out.println("No Valid Process Id specified, killing the program and exiting");
            System.exit(0);
        }
        processId = args[0];
        processId = processId.replaceAll("[\\n\\r\\s]", "");
        
        // ####### checking for the process id ######
        if (!processId.matches("^\\d{1,2}$")) {
            System.out.println("No Valid Process Id specified, killing the program and exiting");
            System.exit(0);
        }
        String myProgram = "WVMS_transMsgInsertProcess.pl";
        
        // ############ Check if process is already running ###############################
        // Execute the command: ps -elf | grep -v grep | grep "myProgram processId"
        List<String> ret = new ArrayList<String>();
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{"sh", "-c", "ps -elf | grep -v grep | grep \"" + myProgram + " " + processId + "\""});
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                ret.add(s);
            }
        } catch(Exception e) {
            // In case of error continue as if no process is running.
        }
        // In Perl, if (0 < $#ret) means if more than one line is returned.
        if (ret.size() > 1) {
            System.out.println("Program " + myProgram + " already running with Process Id " + processId);
            System.exit(0);
        }
        SignalFlag = "Y";
        // ########## for log writing #########
        logTraceFile = "WVMS_transMsgInsertProcess_" + processId + ".log";
        singleLineLog = "WVMS_transMsgInsertProcessStatus.log";
        moduleName = "WVMS_transMsgInsertProcess" + processId;
        // ################## Global declaration ##########
        TransMsgDetPrep = null;
        smsPort = "";
        insertOutSMSPrep = null;
        fetchMsgPrep = null;
        fetchAmtPrep = null;
        updateTransStat = null;
        sendSmsFlag = "";
        
        // ############### user defined signals for closure ##########
        // (Signals already handled above)

        while (true) {
            try {
                generic.logfunction(logTraceFile, moduleName + " " + processId + " Connecting to Database*** ");
                conDb = new WVMS_perlDatabase();
                conDb.dbCon();
                generic.logfunction(logTraceFile, " conDb = " + conDb);
                SignalFlag = "N";
                loadQueries();
                generic.logfunction(logTraceFile, " AFTER LOAD QUERIES BEFORE ");
                mainLoop();
            } catch(Exception e) {
                generic.logfunction(logTraceFile, " Exception: " + e.getMessage());
            }
            if (conDb != null) {
                generic.logfunction(logTraceFile, moduleName + " " + processId + " Disconnecting from Database*** ");
                TransMsgDetPrep = null;
                insertOutSMSPrep = null;
                fetchMsgPrep = null;
                fetchAmtPrep = null;
                updateTransStat = null;
                conDb.dbDiscon();
                System.exit(0);
            }
            try {
                Thread.sleep(1000);
            } catch(InterruptedException ie) {
                // continue if interrupted
            }
        }
    }
    
    public static void mainLoop() {
        generic.logfunction(logTraceFile, " mainmainmainmainmain mainmainmainmain ");
        while (true) {
            if (SignalFlag.equals("Y")) {
                generic.logfunction(logTraceFile, moduleName + " " + processId + "  *** inside Signal");
                SignalFlag = "N";
                loadQueries();
            }
            boolean res = conDb.dbExe(TransMsgDetPrep);
            if (!res) {
                // mimic: unless($res) { undef $res; $conDb->dbDiscon(); log error; exit(); }
                conDb.dbDiscon();
                generic.logfunction(logTraceFile, " ERROR " + TransMsgDetPrep.getQuery());
                System.exit(0);
            }
            Object[] row;
            // Simulate fetching rows from the executed query.
            while ((row = TransMsgDetPrep.fetchrow_array()) != null) {
                // Each row: ($seqId,$msgId,$transactionId,$status,$processId,$date,$balDet,$rechargeDet)
                String seqId = row[0].toString();
                String msgId = row[1].toString();
                String transactionId = row[2].toString();
                String status = row[3].toString();
                String procId = row[4].toString();
                String date = row[5].toString();
                String balDet = row[6].toString();
                String rechargeDet = row[7].toString();
                generic.logfunction(logTraceFile, "seqId=" + seqId + "|transactionId=" + transactionId + "|Date=" + date + "|msgId=" + msgId + "|status=" + status);
                
                if (sendSmsFlag.equals("1")) { // ##send SMS
                    Object[] msgInfo = getMsg(msgId, transactionId, balDet, rechargeDet);
                    String msgToSend = msgInfo[0].toString();
                    String voucherAmt = msgInfo[1].toString();
                    String subMsisdn = msgInfo[2].toString();
                    int returnVal = insertIntoOutSMS(msgToSend, transactionId, subMsisdn, processId);
                    
                    // ########updating Trans Status returnVal(0-failed to insert 1-inseerted to out_sms_q) #####
                    updateTransStat.bind_param(1, returnVal);
                    updateTransStat.bind_param(2, transactionId);
                    res = conDb.dbExe(updateTransStat);
                    if (!res) {
                        conDb.dbDiscon();
                        generic.logfunction(logTraceFile, " ERROR " + updateTransStat.getQuery());
                        System.exit(0);
                    }
                } else {
                    updateTransStat.bind_param(1, 1);
                    updateTransStat.bind_param(2, transactionId);
                    res = conDb.dbExe(updateTransStat);
                    if (!res) {
                        conDb.dbDiscon();
                        generic.logfunction(logTraceFile, " ERROR " + updateTransStat.getQuery());
                        System.exit(0);
                    }
                }
            }
            try {
                Thread.sleep(1000);
            } catch(InterruptedException ie) {
                // continue if interrupted
            }
        }
    }
    
    public static Object[] getMsg(String msgId, String transId, String balDet, String rechargeDet) {
        generic.logfunction(logTraceFile, "msgId=" + msgId + "|transId=" + transId + "|balDet=" + balDet);
        fetchAmtPrep.bind_param(1, transId);
        boolean res = conDb.dbExe(fetchAmtPrep);
        if (!res) {
            conDb.dbDiscon();
            generic.logfunction(logTraceFile, " ERROR " + fetchAmtPrep.getQuery());
            System.exit(0);
        }
        Object[] amtRow = fetchAmtPrep.fetchrow_array();
        if (amtRow == null || amtRow.length < 2) {
            conDb.dbDiscon();
            generic.logfunction(logTraceFile, " ERROR fetching amount details");
            System.exit(0);
        }
        String voucherAmt = amtRow[0].toString();
        String subMsisdn = amtRow[1].toString();
        
        fetchMsgPrep.bind_param(1, msgId);
        res = conDb.dbExe(fetchMsgPrep);
        if (!res) {
            conDb.dbDiscon();
            generic.logfunction(logTraceFile, " ERROR " + fetchMsgPrep.getQuery());
            System.exit(0);
        }
        Object[] msgRow = fetchMsgPrep.fetchrow_array();
        if (msgRow == null || msgRow.length < 1) {
            conDb.dbDiscon();
            generic.logfunction(logTraceFile, " ERROR fetching message");
            System.exit(0);
        }
        String msg = msgRow[0].toString();
        // Perform replacements as in Perl code
        msg = msg.replaceAll("__TRANSID__", transId);
        msg = msg.replaceAll("__DET__", rechargeDet);
        msg = msg.replaceAll("__BONUS__", "");
        msg = msg.replaceAll("__AMOUNT__", voucherAmt);
        msg = msg.replaceAll("__BALDET__", balDet);
        msg = msg.replaceAll("__MSISDN__", subMsisdn);
        return new Object[]{msg, voucherAmt, subMsisdn};
    }
    
    public static int insertIntoOutSMS(String msg, String transId, String subMsisdn, String processId) {
        insertOutSMSPrep.bind_param(1, subMsisdn);
        insertOutSMSPrep.bind_param(2, msg);
        insertOutSMSPrep.bind_param(3, transId);
        insertOutSMSPrep.bind_param(4, processId);
        boolean res = conDb.dbExe(insertOutSMSPrep);
        if (!res) {
            conDb.dbDiscon();
            generic.logfunction(logTraceFile, " ERROR " + insertOutSMSPrep.getQuery());
            System.exit(0);
            return 0;
        }
        return 1;
    }
    
    public static void loadQueries() {
        String sql = "SELECT SEQ_ID,MESSAGE_ID,TRANSACTION_ID,STATUS,PROCESS_ID,MSG_DATE,BALANCE_DET,RECHARGE_DET FROM WVMS_TRANSACTION_MESSAGES WHERE STATUS=0 and PROCESS_ID=" + processId + " and ROWNUM<10";
        TransMsgDetPrep = conDb.dbPrep(sql);
        
        smsPort = generic.openConfig("WVMS_SENDSMS_PORT");
        sendSmsFlag = generic.openConfig("WVMS_SEND_MESSAGE_NOTIFICATION_ENABLE");
        
        sql = "INSERT INTO OUT_SMS_Q(MSG_ID,DEST_MSISDN,FROM_MSISDN,MESSAGE,DATE_TIME,MSG_STAT,TRANSACTION_ID,SENDSMS_PORT,OUT_PROCESS_ID) values(OUT_SMS_Q_SEQ.nextval,?,"
                + smsPort + ",?,sysdate,'N',?," + smsPort + ",?)";
        insertOutSMSPrep = conDb.dbPrep(sql);
        
        sql = "select MESSAGE from WVMS_MESSAGE_MAST_1 where MESSAGE_ID=?";
        fetchMsgPrep = conDb.dbPrep(sql);
        
        sql = "select VOUCHER_AMOUNT,SUBSCRIBER_MSISDN,RESP_DESC from transaction_mast where TRANSACTION_ID=?";
        fetchAmtPrep = conDb.dbPrep(sql);
        
        sql = "update WVMS_TRANSACTION_MESSAGES set STATUS=? where TRANSACTION_ID=?";
        updateTransStat = conDb.dbPrep(sql);
        
        return;
    }
    
    public static void closePrg() {
        generic.logfunction(logTraceFile, " closing the program ");
        // In Perl sub, shift(@_) is used to get signal value but here not needed.
        TransMsgDetPrep = null;
        insertOutSMSPrep = null;
        fetchMsgPrep = null;
        fetchAmtPrep = null;
        updateTransStat = null;
        conDb.dbDiscon();
        System.exit(0);
    }
    
    public static void handleSignal() {
        SignalFlag = "Y";
        generic.logfunction(logTraceFile, moduleName + " " + processId + "  *** Got SIG 10... Reloding from config***");
        return;
    }
}

// Class to simulate the WVMS_perlDatabase behavior
class WVMS_perlDatabase {
    private Connection connection;
    // Constructor
    public WVMS_perlDatabase() {
        connection = null;
    }
    public void dbCon() {
        // For simulation, we will not create an actual DB connection.
        // In a real-world scenario, you might connect to a database using JDBC.
        // Example: connection = DriverManager.getConnection(dbURL, user, pass);
        // Here we simulate success.
    }
    public void dbDiscon() {
        // Disconnect from the simulated database.
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // Log error if needed.
            }
        }
        connection = null;
    }
    public QueryPrepared dbPrep(String sql) {
        // Return a new QueryPrepared with the SQL provided.
        return new QueryPrepared(sql);
    }
    public boolean dbExe(QueryPrepared prep) {
        // In this simulation, we simply mark the query as executed.
        // For specific SQL queries, we simulate the result set.
        prep.execute();
        return true;
    }
}

// Class to simulate prepared statement functionality and result set fetching.
class QueryPrepared {
    private String sql;
    private Map<Integer, Object> parameters;
    private boolean executed;
    // For simulation purposes, we use a counter to simulate rows for TransMsgDetPrep.
    private boolean fetchedOnce;
    
    public QueryPrepared(String sql) {
        this.sql = sql;
        this.parameters = new HashMap<Integer, Object>();
        this.executed = false;
        this.fetchedOnce = false;
    }
    
    public String getQuery() {
        return sql;
    }
    
    public void bind_param(int index, Object value) {
        parameters.put(index, value);
    }
    
    public void execute() {
        executed = true;
        // Reset fetched flag on execution for queries which return rows
        fetchedOnce = false;
    }
    
    // Simulate fetchrow_array:
    // For TransMsgDetPrep, we simulate one row then no more rows.
    // For fetchMsgPrep and fetchAmtPrep, we simulate a single row always.
    public Object[] fetchrow_array() {
        // Check the SQL to decide what dummy data to return.
        if (sql.startsWith("SELECT SEQ_ID")) {
            // This is TransMsgDetPrep.
            if (!fetchedOnce) {
                fetchedOnce = true;
                // Return a dummy row with 8 columns:
                // seqId, msgId, transactionId, status, processId, date, balDet, rechargeDet
                return new Object[]{"1", "MSG100", "TRANS100", "0", WVMS_voucherRedemption.processId, "2023-10-10", "BalanceDetail", "RechargeDetail"};
            } else {
                return null;
            }
        } else if (sql.startsWith("select MESSAGE from WVMS_MESSAGE_MAST_1")) {
            // This is fetchMsgPrep.
            // Return a dummy message with placeholders.
            return new Object[]{"Your voucher __TRANSID__ of amount __AMOUNT__ for __MSISDN__ is processed on __DET__. Balance: __BALDET__"};
        } else if (sql.startsWith("select VOUCHER_AMOUNT")) {
            // This is fetchAmtPrep.
            // Return dummy voucher amount and subscriber msisdn. The third column RESP_DESC is not used.
            return new Object[]{"100", "9876543210", "OK"};
        } else {
            // For update and insert queries or others, no result set.
            return null;
        }
    }
}

// Class to simulate WVMS_generic functionality.
class WVMS_generic {
    public void logfunction(String logFile, String message) {
        // For simulation, print log messages to console with the log file name.
        System.out.println("LOG (" + logFile + "): " + message);
    }
    public String openConfig(String configKey) {
        // Simulate configuration reading.
        if (configKey.equals("WVMS_SENDSMS_PORT")) {
            return "1234";
        } else if (configKey.equals("WVMS_SEND_MESSAGE_NOTIFICATION_ENABLE")) {
            return "1";
        }
        return "";
    }
}
  

