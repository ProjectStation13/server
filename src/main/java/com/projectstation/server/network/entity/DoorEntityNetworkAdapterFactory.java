package com.projectstation.server.network.entity;

import com.projectstation.network.IClientVisit;
import com.projectstation.network.WorldVisit;
import com.projectstation.network.command.client.ClientWorldVisit;
import com.projectstation.network.command.world.CreateEntityCommand;
import com.projectstation.network.command.world.SetDoorStateCommand;
import com.projectstation.network.command.world.SetEntityPositionCommand;
import com.projectstation.network.entity.*;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.config.json.JsonVariable;
import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.world.physics.IPhysicsBodyOrientationObserver;
import io.github.jevaengine.world.steering.ISteeringBehavior;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DoorEntityNetworkAdapterFactory implements IEntityNetworkAdapterFactory<IServerEntityNetworkAdapter, Door> {

    @Override
    public IServerEntityNetworkAdapter create(Door e, EntityConfigurationDetails config, IEntityNetworlAdapterHost pr) {
        return new DoorEntityNetworkAdapter(e, config, pr);
    }
}

