package it.mikeslab.truebank.data.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import it.mikeslab.truebank.pojo.database.URIBuilder;
import org.bson.Document;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.Map;

/**
 * Interface: MongoDBService
 * Handles the MongoDB service for the plugin.
 */

public interface MongoDBService {


    void connect(URIBuilder connectionString);

    void disconnect();

    boolean isConnected(boolean silent);
    MongoClient getConnection();


    void setDatabase(String database);

    MongoDatabase getDatabase();


    void setCollection(String collection);

    MongoCollection<Document> getCollection();


    String save(Object obj, Object... args);

    Map.Entry<String, Object> update(String id, Object obj);

    Map.Entry<String, Object> find(Document document, Class<?> clazz);

    void delete(String id);

    Document toDocument(ConfigurationSerializable serializable);

    String getCollectionName();

    void setClass(Class<?> clazz);





}
