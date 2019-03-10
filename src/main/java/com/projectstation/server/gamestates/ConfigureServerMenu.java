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
package com.projectstation.server.gamestates;

import com.jevaengine.spacestation.IState;
import com.jevaengine.spacestation.IStateContext;
import com.jevaengine.spacestation.gamestates.Loading;
import com.jevaengine.spacestation.gamestates.Playing;
import com.projectstation.server.ServerConfig;
import io.github.jevaengine.FutureResult;
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.Button.IButtonPressObserver;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.world.IParallelWorldFactory;
import io.github.jevaengine.world.IWorldFactory.WorldConstructionException;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntityFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class ConfigureServerMenu implements IState
{
	private IStateContext m_context;
	private final Logger m_logger = LoggerFactory.getLogger(ConfigureServerMenu.class);
	private final ServerConfig m_serverConfig;

	public ConfigureServerMenu(ServerConfig config) {
		m_serverConfig = config;
	}

	@Override
	public void enter(IStateContext context)
	{
		m_context = context;

		m_context.setState(new Loading(m_serverConfig.world, new Loading.ILoadingWorldHandler() {
			@Override
			public void done(FutureResult<World, WorldConstructionException> world) {
				try
				{
					m_context.setState(new SimulateServerWorld(m_serverConfig, world.get()));
				} catch (WorldConstructionException e)
				{
					m_logger.error("Unable to enter playing state due to error in loading world", e);
				}
			}
		}));
		
	}

	@Override
	public void leave()
	{
	}

	@Override
	public void update(int iDelta) { }
}
