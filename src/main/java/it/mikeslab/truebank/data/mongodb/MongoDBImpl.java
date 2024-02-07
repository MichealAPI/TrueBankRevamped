package it.mikeslab.truebank.data.mongodb;

import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import it.mikeslab.truebank.pojo.database.URIBuilder;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.Map;

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


        // Create a new client and connect to the server

        this.mongoClient = MongoClients.create(settings);
        if(isConnected(false)) {

            // TODO: Implement a proper logging system,
            //       here should be a message that the connection was successful

        }


        // TODO: If the connection fails, print an error message
    }





    /**
     * Disconnects from the MongoDB server.
     * @return True if the disconnection is successful, false otherwise.
     */
    @Override
    public boolean disconnect() {

        if (this.mongoClient != null) {
            this.mongoClient.close();
            this.mongoClient = null; // Preventing memory leaks
            return true;
        }

        // TODO: Implement a proper logging system
        System.err.println("The MongoDB client is already disconnected.");
        return false;
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

            // TODO: Implement a proper logging system
            if(!silent) {
                System.out.println("Pinged your deployment. You successfully connected to MongoDB!");
            }

            return true;

        } catch (MongoException me) {
            // TODO: Implement a proper logging system
            System.err.println(me);
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


    @Override
    public MongoCollection<Document> getCollection() {
        return this.mongoClient
                .getDatabase(this.database)
                .getCollection(this.collection);
    }

    @Override
    public String save(Object obj, Object... args) {

        ConfigurationSerializable serializable = (ConfigurationSerializable) obj;

        Document document = toDocument(serializable);

        // TODO: Check this functionality, not sure if it's correct

        this.getCollection().insertOne(document);

        return this.find(document, this.entityClass).getKey();
    }

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

    @Override
    public void delete(String id) {

        this.getCollection().deleteOne(new Document("_id", id));

    }

    @Override
    public Map.Entry<String, Object> find(Document document, Class<?> clazz) {
        MongoCollection<Document> collection = this.getCollection();

        Document theDocument = collection
                .find(document)
                .first();

        if (theDocument == null) {
            return null;
        }

        String id = theDocument.getObjectId("_id").toString();

        return Map.entry(id, collection.find(theDocument, clazz).first());
    }


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

    @Override
    public String getCollectionName() {
        return this.collection;
    }

    @Override
    public void setClass(Class<?> clazz) {
        this.entityClass = clazz;
    }

    /**
     * Gets the connection to the MongoDB server.
     * @return The MongoClient instance.
     */
    @Override
    public MongoClient getConnection() {
        return this.mongoClient;
    }
}
