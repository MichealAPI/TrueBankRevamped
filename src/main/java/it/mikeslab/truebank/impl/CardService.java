package it.mikeslab.truebank.impl;

import it.mikeslab.truebank.pojo.Card;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;

public interface CardService {

    /**+
     * Finds a card by its UUID
     * @param uuid the card's UUID
     * @return the card, if found
     */
    Optional<Card> findCard(UUID uuid); // This implementation maybe wrongly implemented, as the UUID management isn't implemented but is basically transformed to String.

    /**
     * Finds a card by its card number
     * @param cardNumber the card's number
     * @return the card, if found
     */
    Optional<Card> findCard(long cardNumber);

    /**
     * Creates a new card
     * @param uuid the card's UUID
     * @param securityCode the card's security code
     * @param cardNumber the card's number
     * @return the created card
     */
    Card createCard(UUID uuid, int securityCode, long cardNumber);

    /**
     * Deletes a card
     * @param uuid the card's UUID
     */
    void deleteCard(UUID uuid);

    /**
     * Checks if a card is valid
     * @param cardNumber the card's number
     * @param securityCode the card's security code
     * @return true if the card is valid, false otherwise
     */
    boolean isCardValid(long cardNumber, int securityCode);

    /**
     * Connects to the card repository
     * @param config the configuration section containing the repository's configuration
     * @return true if the connection was successful, false otherwise
     */
    boolean connectRepository(ConfigurationSection config);


    /**
     * Disconnects from the card repository
     */
    void disconnectRepository();

}
