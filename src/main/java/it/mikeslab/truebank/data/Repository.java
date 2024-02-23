package it.mikeslab.truebank.data;

import org.bson.Document;

import java.util.Map;

/**
 * Generic Repository Interface.
 * Author: Michele C.
 *
 * This interface enables data handling and storage through different sources.
 *
 */
public interface Repository<T> {

    /**
     * Saves an object in the repository.
     *
     * @param obj The object to be saved.
     * @param args Additional arguments for the save operation, strictly
     *             related to EntityStyle.
     */
    String save(T obj, @Deprecated Object... args);

    /**
     * Updates an object in the repository by its id.
     *
     * @param id  The id of the object to be updated.
     * @param obj The updated object.
     */
    void update(String id, T obj);

    /**
     * Deletes an object from the repository by its id.
     *
     * @param id The id of the object to be deleted.
     */
    void delete(String id);

    /**
     * Retrieves an object from the repository by its id and related object.
     *
     * @param id The id of the object to be retrieved.
     * @return The retrieved object.
     */

    T get(String id);


    Map.Entry<String, Object> find(Document document);


    /**
     * Sets the repository name for the repository.
     * Note: This method should be invoked prior to any other operations.
     *       For YAML, it designates the section from which data is read and written.
     *       For MongoDB, it specifies the collection name.
     *       For MySQL, it determines the table name.
     * @param tableName The desired table name.
     */
    void setRepositoryName(String tableName);


    /**
     * Gets the repository name for the repository.
     * @return The table name.
     */
    String getRepositoryName();

    /**
     * Set database columns for the repository.
     * @param columns The columns to be set.
     */
    void setColumns(String[] columns);

    void setType(Class<T> type);

    /**
     * Closes the repository.
     */
    void close();

}
