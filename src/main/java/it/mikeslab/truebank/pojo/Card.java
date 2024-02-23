package it.mikeslab.truebank.pojo;

import lombok.Builder;
import lombok.Data;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.Map;

@Data
@Builder
@SerializableAs("Card")
public class Card implements ConfigurationSerializable {

    // Card type variables

    @BsonProperty("test")
    private int test;

    @BsonProperty("uuid")
    private String uuid;

    @BsonCreator
    public Card(@BsonProperty("test") int test, @BsonProperty("uuid") String uuid) {
        this.test = test;
        this.uuid = uuid;
    }

    public Card(Map<String, ?> map) {
        this.test = (int) map.get("test");
        this.uuid = (String) map.get("uuid");
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of(
                "test", test,
                "uuid", uuid);
    }



}
