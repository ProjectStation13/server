package com.projectstation.server.network.entity;

import com.jevaengine.spacestation.entity.projectile.IProjectile;
import com.projectstation.network.entity.EntityConfigurationDetails;
import com.projectstation.network.entity.IEntityNetworkAdapterFactory;
import com.projectstation.network.entity.IServerEntityNetworkAdapter;

public class ProjectileNetworkAdapterFactory implements IEntityNetworkAdapterFactory<IServerEntityNetworkAdapter, IProjectile> {

    @Override
    public IServerEntityNetworkAdapter create(IProjectile e, EntityConfigurationDetails config, IEntityNetworkAdapterHost pr) {
        return new ProjectileNetworkAdapter(e, config, pr);
    }
}

