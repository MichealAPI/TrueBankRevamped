package it.mikeslab.truebank.data.mongodb;

import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import it.mikeslab.truebank.pojo.database.URIBuilder;
import it.mikeslab.truebank.util.LoggerUtil;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.Map;
import java.util.logging.Level;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoDBImpl implements MongoDBService {

    private MongoClient mongoClient;
    public String collection;
    public String database;
    private Class<?> entityClass;

    // Empty constructor, connection isn't established automatically
    public MongoDBImpl() {

    }

    // Constructor with connection parameters, connection is established automatically
    public MongoDBImpl(URIBuilder uriBuilder) {
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

        // Initialize the database, if not already set
        this.setDatabase(uriBuilder.getDatabase());

        // Construct the connection string (URI)

        String connectionString = "mongodb+srv://" + uriBuilder.getUsername() +
                ":" +
                uriBuilder.getPassword() +
                "@" +
                uriBuilder.getHost();

        // Construct a ServerApi instance using the ServerApi.builder() method
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();


        CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
        CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .codecRegistry(codecRegistry)
                .serverApi(serverApi)
                .build();
        // redundant comment

        // Create a new client and connect to the server

        this.mongoClient = MongoClients.create(settings);

        if(!isConnected(false)) {
            LoggerUtil.log(Level.SEVERE, LoggerUtil.LogSource.DATABASE, "Failed to connect to MongoDB server.");
        }

    }





    /**
     * Disconnects from the MongoDB server.
     */
    @Override
    public void disconnect() {

        if (this.mongoClient != null) {
            this.mongoClient.close();
            this.mongoClient = null; // Preventing memory leaks
            return;
        }

        LoggerUtil.log(Level.WARNING, LoggerUtil.LogSource.DATABASE, "The MongoDB client is already disconnected.");
    }


    /**
     * Checks if the client is connected to the MongoDB server by sending a ping packet.
     * @param silent If true, the method will not print any messages to the console.
     * @return Connection status.
     */

    @Override
    public boolean isConnected(boolean silent) {

        MongoDatabase mongoDatabase = this.getDatabase();

        try {

            // Send a ping to confirm a successful connection
            Bson command = new BsonDocument("ping", new BsonInt64(1));
            Document commandResult = mongoDatabase.runCommand(command);


            if(!silent) {
                LoggerUtil.log(Level.INFO, LoggerUtil.LogSource.DATABASE, "MongoDB server pinged successfully!");
            }

            return true;

        } catch (MongoException me) {
            LoggerUtil.log(Level.SEVERE, LoggerUtil.LogSource.DATABASE, me);
        }

        return false;
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
     * Gets the database for the MongoDB client.
     *
     * @return The database name.
     */
    @Override
    public MongoDatabase getDatabase() {
        return this.mongoClient.getDatabase(this.database);
    }


    /**
     * Sets the collection for the MongoDB client.
     * @param collection The desired collection name.
     */
    @Override
    public void setCollection(String collection) {
        this.collection = collection;
    }


    /**
     * Gets the collection for the MongoDB client.
     * Each call to this method will return a new instance of the collection.
     * @return The collection name.
     */

    @Override
    public MongoCollection<Document> getCollection() {
        return this.mongoClient
                .getDatabase(this.database)
                .getCollection(this.collection);
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

        this.getCollection().insertOne(document);

        return this.find(document, this.entityClass).getKey();

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

        this.getCollection().findOneAndUpdate(
                new Document("_id", id),
                new Document("$set", document)
        );

        return this.find(new Document("_id", id), this.entityClass);
    }





    /**
     * Deletes an object from the MongoDB database.
     * @param id The object's ID.
     */
    @Override
    public void delete(String id) {

        this.getCollection().deleteOne(new Document("_id", id));

    }




    /**
     * Finds an object in the MongoDB database.
     * @param document The query document.
     * @param clazz The object's class, it's projected for a POJO related utilize.
     * @return The object.
     */
    @Override
    public Map.Entry<String, Object> find(Document document, Class<?> clazz) {

        // IDs in MongoDB are stored as "_id", so we need to convert an eventual id key to "_id"
        if (document.containsKey("id")) {
            document.put("_id", document.get("id"));
            document.remove("id");
        }


        MongoCollection<Document> collection = this.getCollection();

        Document theDocument = collection
                .find(document)
                .first();

        if (theDocument == null) {
            return null;
        }

        String id = theDocument.getObjectId("_id").toString();

        return Map.entry(id, collection.find(theDocument, clazz).first()); // todo fix content null
    }





    /**
     * Converts a ConfigurationSerializable object to a Document.
     * @param serializable The ConfigurationSerializable object.
     * @return The Document instance.
     */
    @Override
    public Document toDocument(ConfigurationSerializable serializable) {

        // Initialize a new Document instance
        Document theDocument = new Document();

        // Gets values from the serializable object
        Map<String, Object> serialized = serializable.serialize();

        // Puts all the values into the document
        theDocument.putAll(serialized);

        return theDocument;
    }





    /**
     * Gets the collection name.
     * @return The collection name.
     */
    @Override
    public String getCollectionName() {
        return this.collection;
    }




    /**
     * Sets the class for the MongoDB client.
     * This implementation is though to be used with POJOs.
     * @param clazz The desired class.
     */
    @Override
    public void setClass(Class<?> clazz) {
        this.entityClass = clazz;
    }




    /**
     * Gets the connection to the MongoDB server.
     * @return The cached MongoClient instance.
     */
    @Override
    public MongoClient getConnection() {
        return this.mongoClient;
    }
}
