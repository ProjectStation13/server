package com.projectstation.server.network.entity;

import com.jevaengine.spacestation.entity.Infrastructure;
import com.projectstation.network.entity.EntityNetworkAdapterMapping;
import io.github.jevaengine.rpg.entity.character.DefaultRpgCharacter;

public class ServerNetworkEntityMappings extends EntityNetworkAdapterMapping {
    public ServerNetworkEntityMappings() {

        register(Infrastructure.class, new SimpleEntityNetworkAdapterFactory());
        register(DefaultRpgCharacter.class, new CharacterNetworkAdapterFactory());
    }
}
