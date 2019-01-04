package com.projectstation.server;

import com.jevaengine.spacestation.entity.StationEntityFactory;
import com.projectstation.network.entity.EntityConfigurationDetails;
import io.github.jevaengine.IAssetStreamFactory;
import io.github.jevaengine.config.IConfigurationFactory;
import io.github.jevaengine.config.IImmutableVariable;
import io.github.jevaengine.rpg.item.IItemFactory;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.pathfinding.IRouteFactory;
import io.github.jevaengine.world.scene.model.IAnimationSceneModelFactory;

import java.net.URI;
import java.util.WeakHashMap;

public class ServerStationEntityFactory extends StationEntityFactory {
    private static WeakHashMap<IEntity, EntityConfigurationDetails> entityConfigDetails = new WeakHashMap<>();

    public ServerStationEntityFactory(IEntityFactory base, IItemFactory itemFactory, IConfigurationFactory configurationFactory, IAnimationSceneModelFactory animationSceneModelFactory, IAssetStreamFactory assetStreamFactory, IRouteFactory routeFactory) {
        super(base, itemFactory, configurationFactory, animationSceneModelFactory, assetStreamFactory, routeFactory);
    }

    @Override
    public <T extends IEntity> T create(Class<T> entityClass, String instanceName, URI config) throws EntityConstructionException {
        IEntity e = super.create(entityClass, instanceName, config);
        entityConfigDetails.put(e, new EntityConfigurationDetails(lookup(entityClass), config));
        return (T)e;
    }

    @Override
    public IEntity create(String entityName, String instanceName, URI config) throws EntityConstructionException {
        IEntity e = super.create(entityName, instanceName, config);
        entityConfigDetails.put(e, new EntityConfigurationDetails(entityName, config));
        return e;
    }

    @Override
    public <T extends IEntity> T create(Class<T> entityClass, String instanceName, IImmutableVariable config) throws EntityConstructionException {
        IEntity e = super.create(entityClass, instanceName, config);
        entityConfigDetails.put(e, new EntityConfigurationDetails(lookup(entityClass), config));
        return (T)e;
    }

    @Override
    public <T extends IEntity> T create(Class<T> entityClass, String instanceName) throws EntityConstructionException {
        IEntity e = super.create(entityClass, instanceName);
        entityConfigDetails.put(e, new EntityConfigurationDetails(lookup(entityClass)));
        return (T)e;
    }

    @Override
    public IEntity create(String entityName, String instanceName, IImmutableVariable config) throws EntityConstructionException {
        IEntity e = super.create(entityName, instanceName, config);
        entityConfigDetails.put(e, new EntityConfigurationDetails(entityName, config));
        return e;
    }

    @Override
    public IEntity create(String entityClass, String instanceName) throws EntityConstructionException {
        IEntity e = super.create(entityClass, instanceName);
        entityConfigDetails.put(e, new EntityConfigurationDetails(entityClass));
        return e;
    }

    @Override
    public <T extends IEntity> T create(Class<T> entityClass, String instanceName, URI config, IImmutableVariable auxConfig) throws EntityConstructionException {
        IEntity e = super.create(entityClass, instanceName, config, auxConfig);
        entityConfigDetails.put(e, new EntityConfigurationDetails(lookup(entityClass), config, auxConfig));
        return (T)e;
    }

    @Override
    public IEntity create(String entityClass, String instanceName, URI config, IImmutableVariable auxConfig) throws EntityConstructionException {
        IEntity e = super.create(entityClass, instanceName, config, auxConfig);
        entityConfigDetails.put(e, new EntityConfigurationDetails(entityClass, config, auxConfig));
        return e;
    }

    @Nullable
    public static EntityConfigurationDetails getConfig(IEntity e) {
        return entityConfigDetails.containsKey(e) ? entityConfigDetails.get(e) : null;
    }
}
