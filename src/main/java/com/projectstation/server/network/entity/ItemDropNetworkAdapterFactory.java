package com.projectstation.server.network.entity;

import com.jevaengine.spacestation.entity.ItemDrop;
import com.projectstation.network.entity.EntityConfigurationDetails;
import com.projectstation.network.entity.IEntityNetworkAdapterFactory;
import com.projectstation.network.entity.IServerEntityNetworkAdapter;

public class ItemDropNetworkAdapterFactory implements IEntityNetworkAdapterFactory<IServerEntityNetworkAdapter, ItemDrop> {

    @Override
    public IServerEntityNetworkAdapter create(ItemDrop e, EntityConfigurationDetails config, IEntityNetworkAdapterHost pr) {
        return new ItemDropNetworkAdapter(e, config, pr);
    }
}

