package com.projectstation.server.network.entity;

import com.projectstation.network.entity.*;
import io.github.jevaengine.rpg.entity.Door;

public class DoorEntityNetworkAdapterFactory implements IEntityNetworkAdapterFactory<IServerEntityNetworkAdapter, Door> {

    @Override
    public IServerEntityNetworkAdapter create(Door e, EntityConfigurationDetails config, IEntityNetworkAdapterHost pr) {
        return new DoorEntityNetworkAdapter(e, config, pr);
    }
}

