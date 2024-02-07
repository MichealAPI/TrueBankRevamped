package it.mikeslab.truebank.data.mongodb;

import it.mikeslab.truebank.data.Repository;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.Map;

public class MongoDBRepository<T extends ConfigurationSerializable> implements Repository<T> {

    private final MongoDBService service;
    private Class<T> type;

    public MongoDBRepository(MongoDBService service, Class<T> type) {
        this.service = service;

        // Set the type of the repository, strictly necessary and related to the POJO based approach
        this.setType(type);
    }


    @Override
    public String save(T obj, Object... args) {
        try {
            return service.save(obj, args);
        } catch (Exception e) {
            // Log the exception and rethrow it, or handle it in some other way
            throw new RuntimeException("Failed to save object", e);
        }
    }

    @Override
    public T get(String id) {
        ObjectId objectId = new ObjectId(id);
        Map.Entry<String, Object> entryMap = service.find(new Document("_id", objectId), type);

        if (entryMap == null || !type.isInstance(entryMap.getValue())) {
            return null;
        }

        return type.cast(entryMap.getValue());
    }


    @Override
    public void update(String id, T obj) {
        service.update(id, obj);
    }

    @Override
    public void delete(String id) {
        service.delete(id);
    }


    @Override
    public void setRepositoryName(String tableName) {
        service.setCollection(tableName);
    }

    @Override
    public String getRepositoryName() {
        return service.getCollectionName();
    }

    @Override
    public void setColumns(String[] columns) {
        // Since MongoDB is a NoSQL database, we do not need to set any column
    }

    @Override
    public void setType(Class<T> type) {
        this.type = type;
        this.service.setClass(type);
    }

    @Override
    public void close() {
        service.disconnect();
    }
}