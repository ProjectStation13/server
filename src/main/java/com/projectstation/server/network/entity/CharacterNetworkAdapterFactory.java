package com.projectstation.server.network.entity;

import com.jevaengine.spacestation.entity.character.SpaceCharacter;
import com.projectstation.network.IClientVisit;
import com.projectstation.network.command.client.ClientWorldVisit;
import com.projectstation.network.command.client.GiveEntityNickname;
import com.projectstation.network.command.world.CreateEntityCommand;
import com.projectstation.network.command.world.SetEntityPositionCommand;
import com.projectstation.network.command.world.SetEntityVelocityCommand;
import com.projectstation.network.entity.*;
import io.github.jevaengine.config.ValueSerializationException;
import io.github.jevaengine.config.json.JsonVariable;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.entity.character.IMovementResolver;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.world.physics.IPhysicsBodyOrientationObserver;
import io.github.jevaengine.world.steering.ISteeringBehavior;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CharacterNetworkAdapterFactory implements IEntityNetworkAdapterFactory<IServerEntityNetworkAdapter, SpaceCharacter> {

    @Override
    public IServerEntityNetworkAdapter create(SpaceCharacter e, EntityConfigurationDetails config, IEntityNetworkAdapterFactory.IEntityNetworlAdapterHost pr) {
        return new CharacterNetworkAdapter(e, config, pr);
    }
}

