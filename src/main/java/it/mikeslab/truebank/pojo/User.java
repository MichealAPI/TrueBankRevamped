package it.mikeslab.truebank.pojo;

import lombok.Builder;
import lombok.Data;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.UUID;

/*
    Class: User
    Handles basic user data for the bank system.
*/

@Data
@Builder
public class User {

    // Player uuid

    @BsonProperty("uuid")
    private final UUID uuid;

    // Player display name, if any

    @BsonProperty("displayName")
    private String displayName;

    // Player Card's Security Code, necessary for registration

    @BsonProperty("securityCode")
    private int securityCode;

    // Player Card's Number, necessary for registration

    @BsonProperty("cardNumber")
    private long cardNumber;

    // Player Debit Card type, necessary for registration

    @BsonProperty("card")
    private Card card;

    // Player balance, defaults to 0, can be updated

    @BsonProperty("balance")
    private double balance;



}
