package com.projectstation.server.scene.model;

import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.graphics.NullGraphic;
import io.github.jevaengine.graphics.Sprite;

import java.net.URI;

public class NullSpriteFactory implements ISpriteFactory {
    @Override
    public Sprite create(URI path) throws SpriteConstructionException {
        return new Sprite(new NullGraphic(), 1.0f);
    }
}
