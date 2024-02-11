package it.mikeslab.truebank.data.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.mikeslab.truebank.pojo.database.URIBuilder;
import org.bson.Document;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.sql.*;
import java.util.*;

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
     * Connects to the MongoDB server using the provided credentials.
     * Reference: https://www.mongodb.com/docs/drivers/java/sync
     *
     * @param uriBuilder The connection string.
     */

    @Override
    public void connect(URIBuilder uriBuilder) {

        HikariConfig config = new HikariConfig();

        // JDBC URL needed in case of non-standard ports
        StringBuilder jdbcUrl = new StringBuilder("jdbc:mysql://");
        jdbcUrl.append(uriBuilder.getHost());

        if (uriBuilder.getPort() != 0) {
            jdbcUrl.append(":");
            jdbcUrl.append(uriBuilder.getPort());
        }

        jdbcUrl.append("/");
        jdbcUrl.append(uriBuilder.getDatabase());

        config.setJdbcUrl(jdbcUrl.toString());

        config.setUsername(uriBuilder.getUsername());

        if(uriBuilder.getPassword() != null) // todo: check if this is the right way to check for null
            config.setPassword(uriBuilder.getPassword());

        this.sqlClient = new HikariDataSource(config);

        // TODO: If the connection fails, print an error message
    }





    /**
     * Disconnects from the MongoDB server.
     */
    @Override
    public void disconnect() {

        if (this.sqlClient != null) {
            this.sqlClient.close();
            this.sqlClient = null; // Preventing memory leaks
            return;
        }

        // TODO: Implement a proper logging system
        System.err.println("The MongoDB client is already disconnected.");
    }


    /**
     * Checks if the client is connected to the MongoDB server by sending a ping packet.
     * @param silent If true, the method will not print any messages to the console.
     * @return Connection status.
     */

    @Override
    public boolean isConnected(boolean silent) {
        try {
            boolean isConnected = this.sqlClient.isRunning();
            if(!silent) {
                System.out.println("Connection status: " + (isConnected ? "Connected" : "Disconnected"));
            }
            return isConnected;
        } catch (Exception e) {
            System.err.println(e); // Todo: Implement a proper logging system
            return false;
        }
    }


    /**
     * Sets the database for the MongoDB client.
     * @param database The desired database name.
     */
    @Override
    public void setDatabase(String database) {
        this.database = database;
    }



    /**
     * Sets the collection for the MongoDB client.
     * @param table The desired collection name.
     */
    @Override
    public void setTable(String table) {
        this.table = table;
    }


    /**
     * Gets the collection for the MongoDB client.
     * Each call to this method will return a new instance of the collection.
     * @return The collection name.
     */

    @Override
    public String getTable() {
        return this.table;
    }


    /**
     * Saves an object to the MongoDB database.
     * In this implementation, the first arg should contain the
     * @param obj The object to be saved.
     * @param args Additional arguments.
     * @return The object's ID.
     */
    @Override
    public String save(Object obj, Object... args) {
        ConfigurationSerializable serializable = (ConfigurationSerializable) obj;
        Document document = toDocument(serializable);

        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        List<Object> parameters = new ArrayList<>();

        for (Map.Entry<String, Object> entry : document.entrySet()) {
            if (columns.length() > 0) {
                columns.append(", ");
                values.append(", ");
            }
            columns.append(entry.getKey());
            values.append("?");
            parameters.add(entry.getValue());
        }

        String sql = "INSERT INTO " + this.table + " (" + columns + ") VALUES (" + values + ")";

        try (Connection connection = this.sqlClient.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getString(1);
                } else {
                    throw new SQLException("Creating item failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            System.err.println(e);
            return null;
        }
    }





    /**
     * Updates an object in the MongoDB database.
     * @param id The object's ID.
     * @param obj The object to be updated.
     * @return The updated object.
     */
    @Override
    public Map.Entry<String, Object> update(String id, Object obj) {
        ConfigurationSerializable serializable = (ConfigurationSerializable) obj;
        Document document = toDocument(serializable);

        StringBuilder setClause = new StringBuilder();
        List<Object> parameters = new ArrayList<>();

        for (Map.Entry<String, Object> entry : document.entrySet()) {
            if (setClause.length() > 0) {
                setClause.append(", ");
            }
            setClause.append(entry.getKey()).append(" = ?");
            parameters.add(entry.getValue());
        }

        parameters.add(id); // Add id at the end of parameters

        String sql = "UPDATE " + this.table + " SET " + setClause + " WHERE id = ?";

        try (Connection connection = this.sqlClient.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }

            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e);
        }

        return find(id);
    }




    /**
     * Deletes an object from the MongoDB database.
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
     * Finds an object in the MongoDB database.
     * @return The object.
     */
    @Override
    public Map.Entry<String, Object> find(String id) {
        try (Connection connection = this.sqlClient.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM " + this.table + " WHERE id = ?")) {
            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    Document document = new Document();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = resultSet.getObject(i);
                        document.put(columnName, value);
                    }

                    ConfigurationSerializable serializable = fromDocument(document);

                    for(Map.Entry<String, Object> entry : document.entrySet()) {
                        System.out.println(entry.getKey() + " : " + entry.getValue());
                    }

                    return new AbstractMap.SimpleEntry<>(id, serializable);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error executing SQL query: " + e.getMessage());
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
     * Sets the class for the MongoDB client.
     * This implementation is though to be used with POJOs.
     * @param clazz The desired class.
     */
    @Override
    public void setClass(Class<? extends ConfigurationSerializable> clazz) {
        this.entityClass = clazz;
    }




    /**
     * Gets the connection to the MongoDB server.
     * @return The cached MongoClient instance.
     */
    @Override
    public HikariDataSource getConnection() {
        return this.sqlClient;
    }

    @Override // todo: add this in the service interface of mysql
    public ConfigurationSerializable fromDocument(Document document) {

        // Puts all the entries of the document into a new map
        Map<String, ?> map = new HashMap<>(document);
        map.remove("id"); // Remove the id from the map, otherwise, entityClass which doesn't contain id will throw an exception

        for(Map.Entry<String, ?> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " B: " + entry.getValue());
        }


        System.out.println("Entity class: " + this.entityClass.getName());
        ConfigurationSerializable serializable = null;
        try {
            serializable = ConfigurationSerialization.deserializeObject(map, this.entityClass);

            for (Map.Entry<String, Object> entry : serializable.serialize().entrySet()) {
                System.out.println(entry.getKey() + " A: " + entry.getValue());
            }
        } catch (Exception e) {
            System.err.println(e);
        }

        return serializable;
    }



    @Override
    public Document toDocument(ConfigurationSerializable serializable) {
        return new Document(serializable.serialize());
    }
}
