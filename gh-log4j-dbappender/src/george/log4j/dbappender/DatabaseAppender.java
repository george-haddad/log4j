package george.log4j.dbappender;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;

/**
 * @author Georges El-Haddad - george.dma@gmail.com
 */
public class DatabaseAppender extends AppenderSkeleton implements Appender {

        private static final String SQL_INSERT_LOG4JTABLE = "insert into log4jTable (id, logger, level, message, stacktrace, ip) values(default,?,?,?,?,?)";

        private PreparedStatement pstmtLog = null;
        private Connection connection = null;
        private String user = null;
        private String password = null;
        private String url = null;
        private static InetAddress inetAddress = null;
        private static final BlockingQueue<LoggingEvent> loggingEventQueue = new LinkedBlockingQueue<LoggingEvent>();
        private static DatabaseAppender instance;

        static {
                try {
                        inetAddress = InetAddress.getLocalHost();
                }
                catch (UnknownHostException e) {
                        inetAddress = null;
                }

                Thread thread = new Thread(new Runnable() {
                        public void run() {
                                processQueue();
                        }
                });

                thread.setDaemon(true);
                thread.start();
        }

        public DatabaseAppender() {
                super();
                instance = this;
        }

        @Override
        protected void append(LoggingEvent event) {
                loggingEventQueue.add(event);
        }

        private void processEvent(LoggingEvent event) {
                Object logMsg = event.getMessage();
                if (logMsg instanceof Serializable) {
                        if (logMsg instanceof String) {
                                try {
                                        String msg = (String) logMsg;
                                        insertLog(event, msg);
                                }
                                catch (SQLException e) {
                                        errorHandler.error("Failed to excute sql", e, ErrorCode.FLUSH_FAILURE);
                                }
                        }
                }

                logMsg = null;
        }

        private void insertLog(LoggingEvent event, String msg) throws SQLException {
                PreparedStatement pstmt = getPstmtLog();
                pstmt.setString(1, event.getLoggerName());
                pstmt.setString(2, event.getLevel().toString());
                pstmt.setString(3, msg);

                if (event.getThrowableInformation() != null) {
                        pstmt.setString(4, ThrowableUtil.getStacktrace(event.getThrowableInformation().getThrowable()));
                }
                else {
                        pstmt.setNull(4, Types.VARCHAR);
                }

                if (inetAddress != null) {
                        pstmt.setString(5, inetAddress.getHostAddress());
                }
                else {
                        pstmt.setString(5, "0.0.0.0");
                }

                pstmt.executeUpdate();
        }

        @Override
        public void activateOptions() {
                try {
                        pstmtLog = getConnection().prepareStatement(SQL_INSERT_LOG4JTABLE);
                        super.activateOptions();
                }
                catch (SQLException sqle) {
                        errorHandler.error("Error while activating options for appender named [" + name + "].", sqle, ErrorCode.GENERIC_FAILURE);
                }
                catch (Exception e) {
                        errorHandler.error("Error while activating options for appender named [" + name + "].", e, ErrorCode.GENERIC_FAILURE);
                }
        }

        private PreparedStatement getPstmtLog() throws SQLException {
                if (pstmtLog == null) {
                        pstmtLog = getConnection().prepareStatement(SQL_INSERT_LOG4JTABLE);
                }

                return pstmtLog;
        }

        private Connection getConnection() throws SQLException {
                if (connection == null || connection.isClosed()) {
                        connection = null;
                        connection = newConnection();
                }

                return connection;
        }

        private Connection newConnection() throws SQLException {
                if (!DriverManager.getDrivers().hasMoreElements())
                        setDriver("sun.jdbc.odbc.JdbcOdbcDriver");

                if (connection == null || connection.isClosed()) {
                        connection = null;
                        connection = DriverManager.getConnection(url, user, password);
                }

                return connection;
        }

        public void setDriver(String driverClass) {
                try {
                        Class.forName(driverClass);
                }
                catch (Exception e) {
                        errorHandler.error("Failed to load driver", e, ErrorCode.GENERIC_FAILURE);
                }
        }

        public void close() {
                try {
                        if (pstmtLog != null) {
                                pstmtLog.close();
                        }

                        if (connection != null) {
                                if (!connection.isClosed()) {
                                        connection.close();
                                }
                        }
                }
                catch (SQLException e) {
                        errorHandler.error("Error closing connection", e, ErrorCode.GENERIC_FAILURE);
                }
                finally {
                        pstmtLog = null;
                        connection = null;
                }

                this.closed = true;
        }

        private static void processQueue() {
                while (true) {
                        try {
                                LoggingEvent event = loggingEventQueue.poll(1L, TimeUnit.SECONDS);
                                if (event != null) {
                                        instance.processEvent(event);
                                }
                        }
                        catch (InterruptedException e) {
                                // No operations.
                        }
                }
        }

        public boolean requiresLayout() {
                return false;
        }

        public void setUser(String user) {
                this.user = user;
        }

        public void setPassword(String password) {
                this.password = password;
        }

        public void setURL(String url) {
                this.url = url;
        }

        @Override
        public void finalize() {
                close();
                super.finalize();
        }
}
