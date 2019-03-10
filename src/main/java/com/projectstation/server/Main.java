/* 
 * Copyright (C) 2015 Jeremy Wildsmith.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.projectstation.server;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.jevaengine.spacestation.SpaceStationFactory;
import com.jevaengine.spacestation.StationAssetStreamFactory;
import com.jevaengine.spacestation.entity.StationEntityFactory;
import com.jevaengine.spacestation.entity.character.SpaceCharacterFactory;
import com.jevaengine.spacestation.item.StationItemFactory;
import com.jevaengine.spacestation.ui.StationControlFactory;
import com.projectstation.server.entity.ServerStationEntityFactory;
import com.projectstation.server.item.ServerStationItemFactory;
import com.projectstation.server.scene.model.NullSpriteFactory;
import io.github.jevaengine.IAssetStreamFactory;
import io.github.jevaengine.IEngineThreadPool;
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.audio.NullAudioClipFactory;
import io.github.jevaengine.config.CachedConfigurationFactory;
import io.github.jevaengine.config.IConfigurationFactory;
import io.github.jevaengine.config.json.JsonConfigurationFactory;
import io.github.jevaengine.game.FrameRenderer;
import io.github.jevaengine.game.FrameRenderer.RenderFitMode;
import io.github.jevaengine.game.GameDriver;
import io.github.jevaengine.game.IGameFactory;
import io.github.jevaengine.game.IRenderer;
import io.github.jevaengine.graphics.*;
import io.github.jevaengine.joystick.FrameInputSource;
import io.github.jevaengine.joystick.IInputSource;
import io.github.jevaengine.joystick.NullInputSource;
import io.github.jevaengine.rpg.dialogue.IDialogueRouteFactory;
import io.github.jevaengine.rpg.dialogue.ScriptedDialogueRouteFactory;
import io.github.jevaengine.rpg.entity.RpgEntityFactory;
import io.github.jevaengine.rpg.entity.character.IRpgCharacterFactory;
import io.github.jevaengine.rpg.item.IItemFactory;
import io.github.jevaengine.rpg.ui.RpgControlFactory;
import io.github.jevaengine.script.IScriptBuilderFactory;
import io.github.jevaengine.ui.DefaultControlFactory;
import io.github.jevaengine.ui.IControlFactory;
import io.github.jevaengine.world.IEffectMapFactory;
import io.github.jevaengine.world.IWorldFactory;
import io.github.jevaengine.world.TiledEffectMapFactory;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.pathfinding.AStarRouteFactory;
import io.github.jevaengine.world.pathfinding.IRouteFactory;
import io.github.jevaengine.world.scene.camera.NullRenderer;
import io.github.jevaengine.world.scene.model.*;
import io.github.jevaengine.world.scene.model.particle.DefaultParticleEmitterFactory;
import io.github.jevaengine.world.scene.model.particle.IParticleEmitterFactory;
import io.github.jevaengine.world.scene.model.sprite.SpriteSceneModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;

public class Main implements WindowListener
{
	private final Logger m_logger = LoggerFactory.getLogger(Main.class);
	private GameDriver m_gameDriver;
	
	public static void main(String[] args)
	{
		System.setProperty("sun.java2d.opengl", "true");
		Main m = new Main();
		m.entry(args);
	}

	public void entry(String[] args)
	{
		m_logger.info("Initializing Server Components");
		Injector injector = Guice.createInjector(new EngineModule());
		m_gameDriver = injector.getInstance(GameDriver.class);
		m_logger.info("Starting Server...");
		m_gameDriver.begin();
	}

	@Override
	public void windowClosing(WindowEvent arg0)
	{
		m_gameDriver.stop();
	}

	private final class EngineModule extends AbstractModule
	{
		@Override
		protected void configure()
		{
			bind(IRouteFactory.class).to(AStarRouteFactory.class);
			bind(IInputSource.class).toInstance(new NullInputSource());
			bind(IGameFactory.class).to(ServerStationGameFactory.class);
			bind(IRpgCharacterFactory.class).to(SpaceCharacterFactory.class);
			bind(IEntityFactory.class).toProvider(new Provider<IEntityFactory>() {
				@Inject
				IEngineThreadPool threadPool;

				@Inject
				private IScriptBuilderFactory scriptBuilderFactory;

				@Inject
				private IAudioClipFactory audioClipFactory;

				@Inject
				private IConfigurationFactory configurationFactory;

				@Inject
				private IRpgCharacterFactory characterFactory;

				@Inject
				private IParticleEmitterFactory particleEmitterFactory;

				@Inject
				private IAnimationSceneModelFactory animationSceneModelFactory;

				@Inject
				private IAssetStreamFactory assetStreamFactory;

				@Inject
				private IItemFactory itemFactory;

				@Inject
				private IRouteFactory rotueFactory;

				@Inject
				private ISceneModelFactory modelFactory;

				@Override
				public IEntityFactory get() {
					IEntityFactory base0 = new RpgEntityFactory(scriptBuilderFactory, audioClipFactory, configurationFactory, characterFactory, particleEmitterFactory, animationSceneModelFactory, modelFactory);
					IEntityFactory base1 = new StationEntityFactory(base0, itemFactory, configurationFactory, animationSceneModelFactory, assetStreamFactory, rotueFactory);
					return new ServerStationEntityFactory(base1, threadPool, itemFactory, configurationFactory, animationSceneModelFactory, assetStreamFactory, rotueFactory);
				}
			});

			bind(IItemFactory.class).toProvider(new Provider<IItemFactory>() {
				@Inject
				IEngineThreadPool threadPool;

				@Inject
				private IGraphicFactory graphicFactory;

				@Inject
				private IConfigurationFactory configurationFactory;

				@Inject
				private IAnimationSceneModelFactory modelFactory;

				@Inject
				private Provider<IEntityFactory> entityFactory;

				@Override
				public IItemFactory get() {
					IItemFactory base0 = new StationItemFactory(configurationFactory, graphicFactory, modelFactory, entityFactory, this);
					return new ServerStationItemFactory(base0);
				}
			});

			bind(IWorldFactory.class).to(SpaceStationFactory.class);

			bind(IDialogueRouteFactory.class).to(ScriptedDialogueRouteFactory.class);

			bind(IControlFactory.class).toProvider(new Provider<IControlFactory>() {
				@Inject
				private IGraphicFactory graphicFactory;
				
				@Inject
				private IConfigurationFactory configurationFactory;
				
				@Override
				public IControlFactory get() {
					IControlFactory baseFactory = new RpgControlFactory(new DefaultControlFactory(graphicFactory, configurationFactory), configurationFactory);
					return new StationControlFactory(baseFactory, configurationFactory, graphicFactory);
				}
			});
			
			bind(IAudioClipFactory.class).toInstance(new NullAudioClipFactory());
			bind(IEffectMapFactory.class).to(TiledEffectMapFactory.class);
			bind(IRenderer.class).toInstance(new NullRenderer());
			bind(IParticleEmitterFactory.class).to(DefaultParticleEmitterFactory.class);
			
			bind(IAssetStreamFactory.class).toProvider(new Provider<IAssetStreamFactory>() {
				@Override
				public IAssetStreamFactory get() {
					return new StationAssetStreamFactory(Paths.get("").toUri());
				}
			});
			
			bind(IConfigurationFactory.class).toProvider(new Provider<IConfigurationFactory>(){
				@Inject
				private IAssetStreamFactory assetStreamFactory;
				
				@Override
				public IConfigurationFactory get() {
					return new CachedConfigurationFactory(new JsonConfigurationFactory(assetStreamFactory));
				}
			});

			bind(IGraphicFactory.class).toProvider(new Provider<IGraphicFactory>() {
				@Inject
				private IConfigurationFactory configurationFactory;

				@Inject
				private IAssetStreamFactory assetStreamFactory;
				
				@Inject
				private IRenderer renderer;
				
				@Override
				public IGraphicFactory get() {
					ExtentionMuxedGraphicFactory muxedGraphicFactory = new ExtentionMuxedGraphicFactory(new BufferedImageGraphicFactory(renderer, assetStreamFactory));
					IGraphicFactory graphicFactory = new CachedGraphicFactory(muxedGraphicFactory);
					muxedGraphicFactory.put(".sgf", new ShadedGraphicFactory(new DefaultGraphicShaderFactory(this, configurationFactory), graphicFactory, configurationFactory));
					return graphicFactory;
				}
			});


			bind(ISceneModelFactory.class).toProvider(new Provider<ISceneModelFactory>() {
				@Inject
				private IAssetStreamFactory assetStreamFactory;
				
				@Inject
				private ISpriteFactory spriteFactory;
				
				@Inject
				private IConfigurationFactory configurationFactory;
				
				@Inject
				private IAudioClipFactory audioClipFactory;
				
				@Inject
				private IParticleEmitterFactory particleEmitterFactory;
				
				@Override
				public ISceneModelFactory get() {
					ExtentionMuxedSceneModelFactory muxedFactory = new ExtentionMuxedSceneModelFactory(new SpriteSceneModelFactory(configurationFactory, new NullSpriteFactory(), audioClipFactory));
					muxedFactory.put("jpar", particleEmitterFactory);
					
					return muxedFactory;
				}
			});
			
			
			bind(IAnimationSceneModelFactory.class).toProvider(new Provider<IAnimationSceneModelFactory>() {
				@Inject
				private IAssetStreamFactory assetStreamFactory;
				
				@Inject
				private ISpriteFactory spriteFactory;
				
				@Inject
				private IConfigurationFactory configurationFactory;
				
				@Inject
				private IAudioClipFactory audioClipFactory;
				
				@Inject
				private IParticleEmitterFactory particleEmitterFactory;
				
				@Override
				public IAnimationSceneModelFactory get() {
					ExtentionMuxedAnimationSceneModelFactory muxedFactory = new ExtentionMuxedAnimationSceneModelFactory(new SpriteSceneModelFactory(configurationFactory, spriteFactory, audioClipFactory));
					muxedFactory.put("jpar", particleEmitterFactory);
					
					return muxedFactory;
				}
			});
		}
	}
	
	@Override
	public void windowActivated(WindowEvent arg0) { }
	
	@Override
	public void windowClosed(WindowEvent arg0) { }

	@Override
	public void windowDeactivated(WindowEvent arg0) { }

	@Override
	public void windowDeiconified(WindowEvent arg0) { }

	@Override
	public void windowIconified(WindowEvent arg0) { }

	@Override
	public void windowOpened(WindowEvent arg0) { }
}
