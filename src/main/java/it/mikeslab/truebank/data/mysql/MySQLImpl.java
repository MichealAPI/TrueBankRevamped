package it.mikeslab.truebank.data.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.mikeslab.truebank.pojo.database.URIBuilder;
import it.mikeslab.truebank.util.LoggerUtil;
import org.bson.Document;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class MySQLImpl implements MySQLService {

    private HikariDataSource sqlClient;
    public String table;
    public String database;
    private Class<? extends ConfigurationSerializable> entityClass;

    // Empty constructor, connection isn't established automatically
    public MySQLImpl() {

    }

    // Constructor with connection parameters, connection is established automatically
    public MySQLImpl(URIBuilder uriBuilder) {
        this.connect(uriBuilder);
    }


    /**
     * Connects to the MySQL server using the provided credentials.*
     * @param uriBuilder The connection string.
     */

    @Override
    public void connect(URIBuilder uriBuilder) {
        HikariConfig config = new HikariConfig();
        String jdbcUrl = new StringBuilder("jdbc:mysql://")
                .append(uriBuilder.getHost())
                .append(uriBuilder.getPort() != 0 ? ":" + uriBuilder.getPort() : "")
                .append("/")
                .append(uriBuilder.getDatabase())
                .toString();

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(uriBuilder.getUsername());
        config.setPassword(Optional.ofNullable(uriBuilder.getPassword()).orElse(""));

        this.sqlClient = new HikariDataSource(config);
    }





    /**
     * Disconnects from the MySQL server.
     */
    @Override
    public void disconnect() {

        if (this.sqlClient != null) {
            this.sqlClient.close();
            this.sqlClient = null; // Preventing memory leaks
            return;
        }

        LoggerUtil.log(Level.INFO, LoggerUtil.LogSource.DATABASE, "The MySQL client disconnected.");
    }


    /**
     * Checks if the client is connected to the MySQL server by sending a ping packet.
     * @param silent If true, the method will not print any messages to the console.
     * @return Connection status.
     */

    @Override
    public boolean isConnected(boolean silent) {
        boolean isConnected = this.sqlClient.isRunning();

        if(!silent) {
            LoggerUtil.log(Level.INFO, LoggerUtil.LogSource.DATABASE, "Connection status: " + (isConnected ? "Connected" : "Disconnected"));
        }

        return isConnected;
    }


    /**
     * Sets the database for the MySQL client.
     * @param database The desired database name.
     */
    @Override
    public void setDatabase(String database) {
        this.database = database;
    }



    /**
     * Sets the collection for the MySQL client.
     * @param table The desired collection name.
     */
    @Override
    public void setTable(String table) {
        this.table = table;
    }


    /**
     * Gets the collection for the MySQL client.
     * Each call to this method will return a new instance of the collection.
     * @return The collection name.
     */

    @Override
    public String getTable() {
        return this.table;
    }


    /**
     * Saves an object to the MySQL database.
     * In this implementation, the first arg should contain the
     * @param obj The object to be saved.
     * @param args Additional arguments.
     * @return The object's ID.
     */
    @Override
    public String save(Object obj, Object... args) {
        ConfigurationSerializable serializable = (ConfigurationSerializable) obj;
        Document document = toDocument(serializable);

        String sql = buildSqlString("INSERT INTO", document);

        try (Connection connection = this.sqlClient.getConnection();
             PreparedStatement statement = prepareStatement(connection, sql, document)) {

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getString(1);
                } else {
                    LoggerUtil.log(Level.WARNING, LoggerUtil.LogSource.DATABASE, new SQLException("Creating item failed, no ID obtained."));
                }
            }
        } catch (SQLException e) {
            handleSQLException(e);
        }
        return null;
    }





    /**
     * Updates an object in the MySQL database.
     * @param id The object's ID.
     * @param obj The object to be updated.
     * @return The updated object.
     */
    @Override
    public Map.Entry<String, Object> update(String id, Object obj) {
        ConfigurationSerializable serializable = (ConfigurationSerializable) obj;
        Document document = toDocument(serializable);

        String sql = buildSqlString("UPDATE", document) + " WHERE id = ?";

        try (Connection connection = this.sqlClient.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            setParameters(statement, document);
            statement.setString(document.size() + 1, id); // Set id at the end of parameters
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e);
        }

        return find(id);
    }



    /**
     * Deletes an object from the MySQL database.
     * @param id The object's ID.
     */
    @Override
    public void delete(String id) {
        try (Connection connection = this.sqlClient.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM " + this.table + " WHERE id = ?")) {
            statement.setString(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e);
        }
    }



    /**
     * Finds an object in the MySQL database.
     * @return The object.
     */
    @Override
    public Map.Entry<String, Object> find(String id) {
        String sql = "SELECT * FROM " + this.table + " WHERE id = ?";

        try (Connection connection = this.sqlClient.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToEntry(resultSet);
                }
            }
        } catch (SQLException e) {
            handleSQLException(e);
        }

        return null;
    }


    /**
     * Gets the collection name.
     * @return The collection name.
     */
    @Override
    public String getTableName() {
        return this.table;
    }




    /**
     * Sets the class for the MySQL client.
     * This implementation is though to be used with POJOs.
     * @param clazz The desired class.
     */
    @Override
    public void setClass(Class<? extends ConfigurationSerializable> clazz) {
        this.entityClass = clazz;
    }




    /**
     * Gets the connection to the MySQL server.
     * @return The cached MongoClient instance.
     */
    @Override
    public HikariDataSource getConnection() {
        return this.sqlClient;
    }

    @Override
    public ConfigurationSerializable fromDocument(Document document) {

        // Puts all the entries of the document into a new map
        Map<String, ?> map = new HashMap<>(document);

        return ConfigurationSerialization.deserializeObject(map, this.entityClass);
    }



    @Override
    public Document toDocument(ConfigurationSerializable serializable) {
        return new Document(serializable.serialize());
    }



    // Helper function to build SQL query string
    private String buildSqlString(String operation, Document document) {
        StringBuilder sql = new StringBuilder(operation + " " + this.table + " SET ");
        for (String key : document.keySet()) {
            sql.append(key).append(" = ?, ");
        }
        sql.delete(sql.length() - 2, sql.length()); // Remove the last comma and space
        return sql.toString();
    }

    // Helper function to set parameters of PreparedStatement
    private void setParameters(PreparedStatement statement, Document document) throws SQLException {
        int index = 1;
        for (Object value : document.values()) {
            statement.setObject(index++, value);
        }
    }

    // Helper function to prepare a PreparedStatement
    private PreparedStatement prepareStatement(Connection connection, String sql, Document document) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        setParameters(statement, document);
        return statement;
    }

    // Helper function to map a ResultSet to a Map.Entry
    private Map.Entry<String, Object> mapResultSetToEntry(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        Document document = new Document();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            Object value = resultSet.getObject(i);
            document.put(columnName, value);
        }

        ConfigurationSerializable serializable = fromDocument(document);
        return new AbstractMap.SimpleEntry<>(resultSet.getString("id"), serializable);
    }

    // Helper function to handle SQLException
    private void handleSQLException(SQLException e) {
        LoggerUtil.log(Level.SEVERE, LoggerUtil.LogSource.DATABASE, "Error executing SQL query: " + e.getMessage());
    }
}
