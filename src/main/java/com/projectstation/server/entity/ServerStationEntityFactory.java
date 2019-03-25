/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.projectstation.server.entity;

import com.jevaengine.spacestation.ui.selectclass.CharacterClassDescription;
import com.projectstation.network.entity.EntityConfigurationDetails;
import io.github.jevaengine.IAssetStreamFactory;
import io.github.jevaengine.IEngineThreadPool;
import io.github.jevaengine.config.*;
import io.github.jevaengine.rpg.item.IItemFactory;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.entity.IParallelEntityFactory;
import io.github.jevaengine.world.entity.ThreadPooledEntityFactory;
import io.github.jevaengine.world.pathfinding.IRouteFactory;
import io.github.jevaengine.world.scene.model.IAnimationSceneModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Jeremy
 */
public class ServerStationEntityFactory implements IEntityFactory {

	private static final WeakHashMap<IEntity, EntityConfigurationDetails> m_entityConfigDetails = new WeakHashMap<>();

	private final Logger m_logger = LoggerFactory.getLogger(ServerStationEntityFactory.class);
	private static final AtomicInteger m_unnamedEntityCount = new AtomicInteger();

	private final IAssetStreamFactory m_assetStreamFactory;
	private final IConfigurationFactory m_configurationFactory;
	private final IAnimationSceneModelFactory m_animationSceneModelFactory;
	private final IItemFactory m_itemFactory;
	private final IParallelEntityFactory m_parallelEntityFactory;
	private final IEntityFactory m_base;

	private final IRouteFactory m_routeFactory;

	@Inject
	public ServerStationEntityFactory(IEntityFactory base, IEngineThreadPool pool, IItemFactory itemFactory, IConfigurationFactory configurationFactory, IAnimationSceneModelFactory animationSceneModelFactory, IAssetStreamFactory assetStreamFactory, IRouteFactory routeFactory) {
		m_base = base;
		m_configurationFactory = configurationFactory;
		m_animationSceneModelFactory = animationSceneModelFactory;
		m_itemFactory = itemFactory;
		m_assetStreamFactory = assetStreamFactory;
		m_routeFactory = routeFactory;
		m_parallelEntityFactory = new ThreadPooledEntityFactory(this, pool);

	}

	public static EntityConfigurationDetails getConfig(IEntity e) {
		synchronized (m_entityConfigDetails) {
			return m_entityConfigDetails.get(e);
		}
	}

	@Override
	@Nullable
	public Class<? extends IEntity> lookup(String className) {
		for (ServerStationEntity e : ServerStationEntity.values()) {
			if (e.getName().equals(className)) {
				return e.getEntityClass();
			}
		}

		return m_base.lookup(className);
	}

	@Override
	@Nullable
	public <T extends IEntity> String lookup(Class<T> entityClass) {
		for (ServerStationEntity e : ServerStationEntity.values()) {
			if (e.getEntityClass().equals(entityClass)) {
				return e.getName();
			}
		}

		return m_base.lookup(entityClass);
	}

	@Override
	public <T extends IEntity> T create(Class<T> entityClass, @Nullable String instanceName, URI config) throws EntityConstructionException {
		IImmutableVariable configVar = new NullVariable();

		try {
			configVar = m_configurationFactory.create(config);
		} catch (IConfigurationFactory.ConfigurationConstructionException e) {
			m_logger.error("Unable to insantiate configuration for entity. Using null configuration instead.", e);
		}

		T e = create(entityClass, instanceName, config, configVar);
		synchronized (m_entityConfigDetails) {
			m_entityConfigDetails.put(e, new EntityConfigurationDetails(lookup(entityClass), config));
		}
		return e;
	}

	@Override
	public IEntity create(String entityName, @Nullable String instanceName, URI config) throws EntityConstructionException {

		IEntity e = create(entityName, instanceName, config, new NullVariable());

		synchronized (m_entityConfigDetails) {
			m_entityConfigDetails.put(e, new EntityConfigurationDetails(entityName, config));
		}

		return e;
	}

	@Override
	public <T extends IEntity> T create(Class<T> entityClass, @Nullable String instanceName, IImmutableVariable config) throws EntityConstructionException {
		T e = create(entityClass, instanceName, URI.create(""), config);

		synchronized (m_entityConfigDetails) {
			m_entityConfigDetails.put(e, new EntityConfigurationDetails(lookup(entityClass), config));
		}

		return e;
	}

	@Override
	public <T extends IEntity> T create(Class<T> entityClass, @Nullable String instanceName) throws EntityConstructionException {
		T e = create(entityClass, instanceName, new NullVariable());

		synchronized (m_entityConfigDetails) {
			m_entityConfigDetails.put(e, new EntityConfigurationDetails(lookup(entityClass)));
		}

		return e;
	}

	@Override
	public IEntity create(String entityName, @Nullable String instanceName, IImmutableVariable config) throws EntityConstructionException {
		Class<? extends IEntity> entityClass = lookup(entityName);

		if (entityClass == null) {
			throw new EntityConstructionException(instanceName, new UnsupportedEntityTypeException(entityClass));
		}

		IEntity e = create(entityClass, instanceName, config);

		synchronized (m_entityConfigDetails) {
			m_entityConfigDetails.put(e, new EntityConfigurationDetails(entityName, config));
		}
		return e;
	}

	@Override
	public IEntity create(String entityClass, @Nullable String instanceName) throws EntityConstructionException {
		IEntity e = create(entityClass, instanceName, new NullVariable());
		synchronized (m_entityConfigDetails) {
			m_entityConfigDetails.put(e, new EntityConfigurationDetails(entityClass));
		}
		return e;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends IEntity> T create(Class<T> entityClass, @Nullable String instanceName, URI config, IImmutableVariable auxConfig) throws EntityConstructionException {

		String configPath = config.getPath();

		IImmutableVariable varConfig = auxConfig;

		try {
			varConfig = new ImmutableVariableOverlay(varConfig,
					configPath.isEmpty() || configPath.endsWith("/") ? new NullVariable() : m_configurationFactory.create(config));
		} catch (IConfigurationFactory.ConfigurationConstructionException e) {
			m_logger.error("Error occured constructing configuration for entity, ignoring external configuration and using just aux config.", e);
		}

		for (ServerStationEntity e : ServerStationEntity.values()) {
			if (e.getEntityClass().equals(entityClass)) {
				T ent = (T) e.getBuilder().create(this, instanceName == null ? this.getClass().getName() + m_unnamedEntityCount.getAndIncrement() : instanceName,
						config, varConfig);

				synchronized (m_entityConfigDetails) {
					m_entityConfigDetails.put(ent, new EntityConfigurationDetails(lookup(entityClass), config, auxConfig));
				}

				return ent;
			}
		}

		T ent = m_base.create(entityClass, instanceName, config, auxConfig);

		synchronized (m_entityConfigDetails) {
			m_entityConfigDetails.put(ent, new EntityConfigurationDetails(lookup(entityClass), config, auxConfig));
		}

		return ent;
	}

	@Override
	public IEntity create(String entityClass, @Nullable String instanceName, URI config, IImmutableVariable auxConfig) throws EntityConstructionException {
		Class<? extends IEntity> entityClazz = lookup(entityClass);

		if (entityClazz == null) {
			throw new EntityConstructionException(instanceName, new UnsupportedEntityTypeException(entityClass));
		}

		IEntity e = create(entityClazz, instanceName, config, auxConfig);

		synchronized (m_entityConfigDetails) {
			m_entityConfigDetails.put(e, new EntityConfigurationDetails(entityClass, config, auxConfig));
		}

		return e;
	}

	private enum ServerStationEntity {
		RandomSpawnController(CharacterRoleSpawnController.class, "randomSpawnController", new EntityBuilder() {
			@Override
			public IEntity create(ServerStationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws EntityConstructionException {
				try {
					RandomSpawnControllerDeclaration decl = auxConfig.getValue(RandomSpawnControllerDeclaration.class);

					Map<String, CharacterClassDescription> desc = new HashMap<>();

					for(CharacterClassDescription d : entityFactory.m_configurationFactory.create(decl.classDescriptions).getValues(CharacterClassDescription[].class)) {
						desc.put(d.name, d);
					}

					Map<CharacterClassDescription, Integer> limits = new HashMap<>();
					Map<CharacterClassDescription, String> spawnZones = new HashMap<>();

					for(Map.Entry<String, Integer> l : decl.spawnLimits.entrySet()) {
						if(desc.containsKey(l.getKey()))
							limits.put(desc.get(l.getKey()), l.getValue());
					}


					for(Map.Entry<String, String> l : decl.spawnZones.entrySet()) {
						if(desc.containsKey(l.getKey()))
							spawnZones.put(desc.get(l.getKey()), l.getValue());
					}

					return new CharacterRoleSpawnController(instanceName, entityFactory.m_parallelEntityFactory, decl.spawnDepth, spawnZones, limits);
				} catch (ValueSerializationException | IConfigurationFactory.ConfigurationConstructionException e) {
					throw new EntityConstructionException(e);
				}
			}
		});

		private final Class<? extends IEntity> m_class;
		private final String m_name;
		private final EntityBuilder m_builder;

		ServerStationEntity(Class<? extends IEntity> clazz, String name, EntityBuilder builder) {
			m_class = clazz;
			m_name = name;
			m_builder = builder;
		}

		public String getName() {
			return m_name;
		}

		public Class<? extends IEntity> getEntityClass() {
			return m_class;
		}

		private EntityBuilder getBuilder() {
			return m_builder;
		}

		public static abstract class EntityBuilder {

			public abstract IEntity create(ServerStationEntityFactory entityFactory, String instanceName, URI context, IImmutableVariable auxConfig) throws EntityConstructionException;
		}
	}

	public static final class RandomSpawnControllerDeclaration implements ISerializable {
		public float spawnDepth;

		public URI classDescriptions;

		public Map<String, Integer> spawnLimits;

		public Map<String, String> spawnZones;

		@Override
		public void serialize(IVariable target) throws ValueSerializationException {
			target.addChild("spawnDepth").setValue(spawnDepth);
			target.addChild("classDescriptions").setValue(classDescriptions);
			target.addChild("spawnLimits").setValue(spawnLimits);
			target.addChild("spawnZones").setValue(spawnZones);
		}

		@Override
		public void deserialize(IImmutableVariable source) throws ValueSerializationException {
			try {
				spawnDepth = source.getChild("spawnDepth").getValue(Double.class).floatValue();

				String sDesc = source.getChild("classDescriptions").getValue(String.class);
				classDescriptions = new URI(sDesc);

				spawnLimits = new HashMap<>();

				IImmutableVariable limits = source.getChild("spawnLimits");
				for(String className : limits.getChildren()) {
					spawnLimits.put(className, limits.getChild(className).getValue(Integer.class));
				}

				spawnZones = new HashMap<>();
				IImmutableVariable zones = source.getChild("spawnZones");
				for(String className : zones.getChildren()) {
					spawnZones.put(className, zones.getChild(className).getValue(String.class));
				}

			} catch (NoSuchChildVariableException | URISyntaxException ex) {
				throw new ValueSerializationException(ex);
			}
		}
	}
}
