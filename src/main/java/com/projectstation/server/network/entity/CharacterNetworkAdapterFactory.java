package com.projectstation.server.network.entity;

import com.jevaengine.spacestation.entity.character.SpaceCharacter;
import com.projectstation.network.entity.*;

public class CharacterNetworkAdapterFactory implements IEntityNetworkAdapterFactory<IServerEntityNetworkAdapter, SpaceCharacter> {

    @Override
    public IServerEntityNetworkAdapter create(SpaceCharacter e, EntityConfigurationDetails config, IEntityNetworkAdapterHost pr) {
        return new CharacterNetworkAdapter(e, config, pr);
    }
}

