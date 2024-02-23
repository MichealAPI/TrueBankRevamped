package it.mikeslab.truebank.pojo.database;

import it.mikeslab.truebank.data.EntityStyle;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class URIBuilder {

    private final String username, password, host;
    private final int port;

    // Optionals, need to be set after the builder
    private String database;
    private String path;
    private EntityStyle style;

}
