package com.projectstation.server.entity;

import io.github.jevaengine.world.entity.IEntity;

public interface ISpawnController extends IEntity {
    void create(ISpawnControllerListener listener);
}
