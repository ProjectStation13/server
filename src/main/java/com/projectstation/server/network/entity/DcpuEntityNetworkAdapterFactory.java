package com.projectstation.server.network.entity;

import com.jevaengine.spacestation.entity.character.SpaceCharacter;
import com.jevaengine.spacestation.entity.power.Dcpu;
import com.projectstation.network.entity.EntityConfigurationDetails;
import com.projectstation.network.entity.IEntityNetworkAdapterFactory;
import com.projectstation.network.entity.IServerEntityNetworkAdapter;

public class DcpuEntityNetworkAdapterFactory implements IEntityNetworkAdapterFactory<IServerEntityNetworkAdapter, Dcpu> {

    @Override
    public IServerEntityNetworkAdapter create(Dcpu e, EntityConfigurationDetails config, IEntityNetworkAdapterHost pr) {
        return new DcpuEntityNetworkAdapter(e, config, pr);
    }
}

