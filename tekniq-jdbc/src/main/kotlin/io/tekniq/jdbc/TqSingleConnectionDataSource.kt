package io.tekniq.jdbc

import java.io.PrintWriter
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.logging.Logger
import javax.sql.DataSource

/**
 * Used mostly for testing purposes. Cannot imagine a real use case for a single connection data source otherwise. True
 * datasource libraries like vibur-dbcp should be used instead in most cases.
 * @author Sejal Patel
 */
open class TqSingleConnectionDataSource(url: String, username: String? = null, password: String? = null, autoCommit: Boolean = true) : DataSource {
    private val connection: Connection = DriverManager.getConnection(url, username, password).apply {
        setAutoCommit(autoCommit)
    }

    fun close() {
        connection.close()
    }

    override fun setLogWriter(out: PrintWriter?) {
        throw UnsupportedOperationException("setLogWriter(PrintWriter)")
    }

    override fun setLoginTimeout(seconds: Int) {
        throw UnsupportedOperationException("setLoginTimeout(Int)")
    }

    override fun getParentLogger(): Logger {
        throw UnsupportedOperationException("getParentLogger()")
    }

    override fun getLogWriter(): PrintWriter {
        throw UnsupportedOperationException("getLogWriter()")
    }

    override fun getLoginTimeout(): Int = 0

    override fun isWrapperFor(iface: Class<*>?): Boolean = iface!!.isInstance(this)

    override fun <T : Any?> unwrap(iface: Class<T>?): T {
        if (iface!!.isInstance(this)) {
            @Suppress("UNCHECKED_CAST")
            return this as T
        }
        throw SQLException("Connection cannot be unwrapped to ${iface.name}")
    }

    override fun getConnection(): Connection = UncloseableConnection(connection)

    override fun getConnection(username: String?, password: String?): Connection = UncloseableConnection(connection)

    private class UncloseableConnection(connection: Connection) : Connection by connection {
        override fun close() {
        }
    }
}

