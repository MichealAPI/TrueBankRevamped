package it.mikeslab.truebank.data.mysql;

import com.zaxxer.hikari.HikariDataSource;
import it.mikeslab.truebank.pojo.database.URIBuilder;
import org.bson.Document;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.Map;

/**
 * Interface: MongoDBService
 * Handles the MongoDB service for the plugin.
 */

public interface MySQLService {


    void connect(URIBuilder connectionString);

    void disconnect();

    boolean isConnected(boolean silent);
    HikariDataSource getConnection();


    void setDatabase(String database);


    void setTable(String table);

    String getTable();


    String save(Object obj, Object... args);

    Map.Entry<String, Object> update(String id, Object obj);

    Map.Entry<String, Object> find(Document document);

    void delete(String id);

    Document toDocument(ConfigurationSerializable serializable);

    String getTableName();

    void setClass(Class<? extends ConfigurationSerializable> clazz);

    ConfigurationSerializable fromDocument(Document document);



}
