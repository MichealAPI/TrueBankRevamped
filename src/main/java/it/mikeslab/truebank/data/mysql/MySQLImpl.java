package it.mikeslab.truebank.data.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.mikeslab.truebank.pojo.database.URIBuilder;
import org.bson.Document;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.sql.*;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

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
        config.setJdbcUrl("jdbc:mysql://" + uriBuilder.getHost() + "/" + uriBuilder.getDatabase());

        config.setUsername(uriBuilder.getUsername());
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
        String json = document.toJson();

        // todo generation style, different id/generation?

        try (Connection connection = this.sqlClient.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO " + this.table + " (data) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, json);
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
        String json = document.toJson();

        try (Connection connection = this.sqlClient.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE " + this.table + " SET data = ? WHERE id = ?")) {
            statement.setString(1, json);
            statement.setString(2, id);
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
                    String json = resultSet.getString("data");
                    Document document = Document.parse(json);
                    ConfigurationSerializable serializable = fromDocument(document);
                    return new AbstractMap.SimpleEntry<>(id, serializable);
                }
            }
        } catch (SQLException e) {
            System.err.println(e);
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
    @Override //todo fix wildcard
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
        Map<String, Object> map = new HashMap<>(document); // Puts all the entries of the document into a new map

        return ConfigurationSerialization.deserializeObject(map, this.entityClass);
    }



    @Override
    public Document toDocument(ConfigurationSerializable serializable) {
        return new Document(serializable.serialize());
    }
}
