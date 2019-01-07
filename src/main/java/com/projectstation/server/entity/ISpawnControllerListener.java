package com.projectstation.server.entity;

import io.github.jevaengine.world.entity.IEntity;

public interface ISpawnControllerListener {
    void spawnedCharacter(IEntity character);
}
