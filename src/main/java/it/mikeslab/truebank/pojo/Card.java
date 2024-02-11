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

    @BsonCreator
    public Card(@BsonProperty("test") int test) {
        this.test = test;
    }

    public Card(Map<String, ?> map) {
        this.test = (int) map.get("test");
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of("test", test);
    }



}
