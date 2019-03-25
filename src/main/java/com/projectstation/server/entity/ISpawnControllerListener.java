package com.projectstation.server.entity;

import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityFactory;

public interface ISpawnControllerListener {
    void spawnedCharacter(IEntity character);
    void spawnError(IEntityFactory.EntityConstructionException e);
}
