package com.projectstation.server.network.entity;

import com.jevaengine.spacestation.entity.Infrastructure;
import com.jevaengine.spacestation.entity.StationEntityFactory;
import com.projectstation.network.IClientVisit;
import com.projectstation.network.command.client.ClientWorldVisit;
import com.projectstation.network.command.world.CreateEntityCommand;
import com.projectstation.network.command.world.SetEntityAnimationDirection;
import com.projectstation.network.command.world.SetEntityAnimationState;
import com.projectstation.network.command.world.SetEntityPositionCommand;
import com.projectstation.network.entity.*;
import io.github.jevaengine.config.NoSuchChildVariableException;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.config.json.JsonVariable;
import io.github.jevaengine.rpg.entity.RpgEntityFactory;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.physics.IPhysicsBodyOrientationObserver;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;
import io.github.jevaengine.world.steering.ISteeringBehavior;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleEntityNetworkAdapterFactory<T extends IEntity> implements IEntityNetworkAdapterFactory<IServerEntityNetworkAdapter, T> {

    private final DetectTraversable<T> traversable;
    private final boolean interactable;

    public SimpleEntityNetworkAdapterFactory(boolean interactable, DetectTraversable<T> traversable) {
        this.traversable = traversable;
        this.interactable = interactable;
    }

    public SimpleEntityNetworkAdapterFactory(DetectTraversable<T> traversable) {
        this(false, traversable);
    }

    @Override
    public IServerEntityNetworkAdapter create(T e, EntityConfigurationDetails config, IEntityNetworkAdapterHost pr) {
        return new SimpleEntityNetworkAdapter(traversable, e, config, pr);
    }

    public interface DetectTraversable<T> {
        boolean traversable(T entity);
    }
}

class SimpleEntityNetworkAdapter<T extends IEntity> implements IServerEntityNetworkAdapter {
    private final T entity;
    private boolean locationChanged = true;
    private final EntityConfigurationDetails config;
    private final IEntityNetworkAdapterFactory.IEntityNetworkAdapterHost pollRequest;
    private final HashMap<String, IAnimationSceneModel.AnimationSceneModelAnimationState> observedStateChanges = new HashMap<>();
    private final SimpleEntityNetworkAdapterFactory.DetectTraversable<T> traversable;

    private IAnimationSceneModel model = null;

    public SimpleEntityNetworkAdapter(SimpleEntityNetworkAdapterFactory.DetectTraversable<T> traversable, T entity, EntityConfigurationDetails config, IEntityNetworkAdapterFactory.IEntityNetworkAdapterHost pr) {
        this.entity = entity;
        this.traversable = traversable;
        this.entity.getBody().getObservers().add(new LocationObserver());

        if(entity.getModel() instanceof IAnimationSceneModel) {
            model = (IAnimationSceneModel)entity.getModel();
            model.getObservers().add(new ModelObserver());
            for(String a : model.getAnimations()) {
                model.getAnimation(a).getObservers().add(new AnimationObserver(a));
            }
        }

        this.config = config;
        this.pollRequest = pr;
    }

    private List<IClientVisit> createSceneArtifactInitializationSteps() throws EntityNetworkAdapterException {
        try {
            List<IClientVisit> response = new ArrayList<>();
            URI configContext = config.getConfigContext();

            JsonVariable auxConfig = new JsonVariable();
            String typeName = RpgEntityFactory.RpgEntity.SceneArtifact.getName();
            RpgEntityFactory.SceneArtifactDeclaration decl = new RpgEntityFactory.SceneArtifactDeclaration();

            decl.blocking = !traversable.traversable(entity);

            if(entity instanceof Infrastructure) {
                Infrastructure inf = (Infrastructure)entity;
            }

            String modelName = config.getConfigContext().resolve("model.jmf").toString();

            if(config.getAuxConfig().childExists("model")) {
                modelName = config.getConfigContext().resolve(config.getAuxConfig().getChild("model").getValue(String.class)).toString();
            }

            decl.model = modelName;

            ByteArrayOutputStream serializedJson = new ByteArrayOutputStream();
            decl.serialize(auxConfig);
            auxConfig.serialize(serializedJson, false);

            String json = new String(serializedJson.toByteArray());
            response.add(new ClientWorldVisit(new CreateEntityCommand(entity.getInstanceName(), typeName, configContext.toString(), json, entity.getBody().getLocation(), entity.getBody().getDirection())));

            return response;
        } catch(ValueSerializationException | IOException | NoSuchChildVariableException ex) {
            throw new EntityNetworkAdapterException(ex);
        }
    }


    private List<IClientVisit> createInfrastructureInitializationSteps() throws EntityNetworkAdapterException {
        try {
            List<IClientVisit> response = new ArrayList<>();
            URI configContext = config.getConfigContext();

            JsonVariable auxConfig = new JsonVariable();
            String typeName = StationEntityFactory.StationEntity.Infrastructure.getName();
            StationEntityFactory.InfrastructureDeclaration decl = new StationEntityFactory.InfrastructureDeclaration();

            decl.type = new String[0];
            decl.blocking = !traversable.traversable(entity);
            decl.isAirTight = true;
            decl.heatConductivity = 0;
            decl.isTransparent = false;

            if(entity instanceof Infrastructure) {
                Infrastructure inf = (Infrastructure)entity;
                decl.isTransparent = inf.isTransparent();
                decl.type = inf.getInfrastructureTypes();
            }

            String modelName = config.getConfigContext().resolve("model.jmf").toString();

            if(config.getAuxConfig().childExists("model")) {
                modelName = config.getConfigContext().resolve(config.getAuxConfig().getChild("model").getValue(String.class)).toString();
            }

            decl.model = modelName;

            ByteArrayOutputStream serializedJson = new ByteArrayOutputStream();
            decl.serialize(auxConfig);
            auxConfig.serialize(serializedJson, false);

            String json = new String(serializedJson.toByteArray());
            response.add(new ClientWorldVisit(new CreateEntityCommand(entity.getInstanceName(), typeName, configContext.toString(), json, entity.getBody().getLocation(), entity.getBody().getDirection())));

            return response;
        } catch(ValueSerializationException | IOException | NoSuchChildVariableException ex) {
            throw new EntityNetworkAdapterException(ex);
        }
    }

    @Override
    public List<IClientVisit> createInitializeSteps() throws EntityNetworkAdapterException {

        List<IClientVisit> response = new ArrayList<>();

        URI configContext = config.getConfigContext();

        if (configContext == null) {
            configContext = URI.create("file:///");
        }

        if(entity instanceof Infrastructure)
            response.addAll(createInfrastructureInitializationSteps());
        else
            response.addAll(createSceneArtifactInitializationSteps());

        if(model != null) {
            response.add(new ClientWorldVisit(new SetEntityAnimationDirection(entity.getInstanceName(), model.getDirection())));

            for(String name : model.getAnimations()) {
                response.add(new ClientWorldVisit(new SetEntityAnimationState(entity.getInstanceName(), name, model.getAnimation(name).getState())));
            }
        }

        return response;
    }

    @Override
    public List<IClientVisit> poll(int deltaTime) {
        List<IClientVisit> response = new ArrayList<>();

        if(locationChanged) {
            response.add(new ClientWorldVisit(new SetEntityPositionCommand(entity.getInstanceName(), entity.getBody().getLocation(), entity.getBody().getDirection())));

            if(model != null)
                response.add(new ClientWorldVisit(new SetEntityAnimationDirection(entity.getInstanceName(), model.getDirection())));

            locationChanged = false;
        }

        for(Map.Entry<String, IAnimationSceneModel.AnimationSceneModelAnimationState> e : observedStateChanges.entrySet()) {
            response.add(new ClientWorldVisit(new SetEntityAnimationState(entity.getInstanceName(), e.getKey(), e.getValue())));
        }

        observedStateChanges.clear();

        return response;
    }

    @Override
    public void setSteeringBehaviour(ISteeringBehavior behaviour) {

    }

    @Override
    public void setSpeed(float speed) {

    }

    private class LocationObserver implements IPhysicsBodyOrientationObserver {
        @Override
        public void locationSet() {
            pollRequest.poll();
            locationChanged = true;
        }

        @Override
        public void directionSet() {
            pollRequest.poll();
            locationChanged = true;
        }
    }

    @Override
    public boolean isOwner() {
        return pollRequest.isOwner();
    }

    private final class AnimationObserver implements IAnimationSceneModel.IAnimationSceneModelAnimationObserver {
        private final String name;

        public AnimationObserver(String name) {
            this.name = name;
        }

        @Override
        public void event(String name) { }

        @Override
        public void stateChanged(IAnimationSceneModel.AnimationSceneModelAnimationState state) {
            observedStateChanges.put(name, state);
            pollRequest.poll();
        }
    }


    private final class ModelObserver implements ISceneModel.ISceneModelObserver {
        @Override
        public void directionChanged() {
            locationChanged = true;
            pollRequest.poll();
        }
    }
}
