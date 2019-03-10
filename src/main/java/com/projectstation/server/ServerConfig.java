package com.projectstation.server;

import io.github.jevaengine.config.*;
import org.apache.commons.lang.NotImplementedException;

public class ServerConfig implements ISerializable {
    public int port = 7345;
    public String masterHost = "127.0.0.1";
    public int masterPort = 8080;
    public String name = "Unnamed";
    public String description = "No description";
    public int maxPlayers = 30;

    @Override
    public void serialize(IVariable target) throws ValueSerializationException {
        throw new ValueSerializationException(new NotImplementedException());
    }

    @Override
    public void deserialize(IImmutableVariable source) throws ValueSerializationException {
        try {
            port = source.getChild("port").getValue(Integer.class);
            masterHost = source.getChild("master").getChild("host").getValue(String.class);
            masterPort = source.getChild("master").getChild("port").getValue(Integer.class);
            name = source.getChild("name").getValue(String.class);
            description = source.getChild("description").getValue(String.class);
            maxPlayers = source.getChild("max_players").getValue(Integer.class);
        } catch (NoSuchChildVariableException e) {
            throw new ValueSerializationException(e);
        }
    }
}
