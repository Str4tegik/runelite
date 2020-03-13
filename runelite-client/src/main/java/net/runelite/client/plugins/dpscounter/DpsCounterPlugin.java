/*
 * Copyright (c) 2020 Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.dpscounter;

import com.google.inject.Provides;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Hitsplat;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ws.PartyMember;
import net.runelite.client.ws.PartyService;
import net.runelite.client.ws.WSClient;
import org.apache.commons.lang3.ArrayUtils;

@PluginDescriptor(
	name = "DPS Counter",
	description = "Counts damage (per second) by a party",
	enabledByDefault = false
)
@Slf4j
public class DpsCounterPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PartyService partyService;

	@Inject
	private WSClient wsClient;

	@Inject
	private DpsOverlay dpsOverlay;

	@Inject
	private DpsConfig dpsConfig;

	@Getter(AccessLevel.PACKAGE)
	private Boss boss;
	private NPC bossNpc;
	@Getter(AccessLevel.PACKAGE)
	private final Map<String, DpsMember> members = new ConcurrentHashMap<>();
	@Getter(AccessLevel.PACKAGE)
	private final DpsMember total = new DpsMember("Total");

	@Provides
	DpsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DpsConfig.class);
	}

	@Override
	protected void startUp()
	{
		total.reset();
		overlayManager.add(dpsOverlay);
		wsClient.registerMessage(DpsUpdate.class);
	}

	@Override
	protected void shutDown()
	{
		wsClient.unregisterMessage(DpsUpdate.class);
		overlayManager.remove(dpsOverlay);
		members.clear();
		boss = null;
	}

	@Subscribe
	public void onPartyChanged(PartyChanged partyChanged)
	{
		members.clear();
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged interactingChanged)
	{
		Actor source = interactingChanged.getSource();
		Actor target = interactingChanged.getTarget();

		if (source != client.getLocalPlayer())
		{
			return;
		}

		if (target instanceof NPC)
		{
			NPC npc = (NPC) target;
			int npcId = npc.getId();
			Boss boss = Boss.findBoss(npcId);
			if (boss != null)
			{
				this.boss = boss;
				bossNpc = (NPC) target;
			}
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied hitsplatApplied)
	{
		Player player = client.getLocalPlayer();
		Actor actor = hitsplatApplied.getActor();

		Hitsplat hitsplat = hitsplatApplied.getHitsplat();

		switch (hitsplat.getHitsplatType())
		{
			case DAMAGE_ME:
				if (actor == player || !(actor instanceof NPC))
				{
					return;
				}

				int hit = hitsplat.getAmount();
				// Update local member
				PartyMember localMember = partyService.getLocalMember();
				// If not in a party, user local player name
				final String name = localMember == null ? player.getName() : localMember.getName();
				DpsMember dpsMember = members.computeIfAbsent(name, DpsMember::new);

				if (dpsMember.isPaused())
				{
					dpsMember.unpause();
					log.debug("Unpausing {}", dpsMember.getName());
				}

				dpsMember.addDamage(hit);

				// broadcast damage
				if (localMember != null)
				{
					final DpsUpdate specialCounterUpdate = new DpsUpdate(((NPC) actor).getId(), hit);
					specialCounterUpdate.setMemberId(localMember.getMemberId());
					wsClient.send(specialCounterUpdate);
				}
				// apply to total
				break;
			case DAMAGE_OTHER:
				if (actor != player.getInteracting() && actor != bossNpc)
				{
					// only track damage to npcs we are attacking, or is a nearby common boss
					return;
				}
				// apply to total
				break;
			default:
				return;
		}

		unpause();
		total.addDamage(hitsplat.getAmount());
	}

	@Subscribe
	public void onDpsUpdate(DpsUpdate dpsUpdate)
	{
		if (partyService.getLocalMember().getMemberId().equals(dpsUpdate.getMemberId()))
		{
			return;
		}

		String name = partyService.getMemberById(dpsUpdate.getMemberId()).getName();
		if (name == null)
		{
			return;
		}

		unpause();

		DpsMember dpsMember = members.computeIfAbsent(name, DpsMember::new);
		dpsMember.addDamage(dpsUpdate.getHit());
	}

	@Subscribe
	public void onOverlayMenuClicked(OverlayMenuClicked event)
	{
		if (event.getEntry().getMenuAction() == MenuAction.RUNELITE_OVERLAY &&
			event.getEntry().getOption().equals("Reset") &&
			event.getEntry().getTarget().equals("DPS counter"))
		{
			members.clear();
			total.reset();
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		if (boss == null)
		{
			return;
		}

		NPC npc = npcSpawned.getNpc();
		int npcId = npc.getId();
		if (!ArrayUtils.contains(boss.getIds(), npcId))
		{
			return;
		}

		log.debug("Boss has spawned!");
		bossNpc = npc;
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned)
	{
		if (npcDespawned.getNpc() != bossNpc)
		{
			return;
		}

		if (bossNpc.isDead())
		{
			log.debug("Boss has died!");

			if (dpsConfig.autopause())
			{
				pause();
			}
		}

		bossNpc = null;
	}

	private void pause()
	{
		if (total.isPaused())
		{
			return;
		}

		log.debug("Pausing");
		for (DpsMember dpsMember : members.values())
		{
			dpsMember.pause();
		}
		total.pause();
	}

	private void unpause()
	{
		if (!total.isPaused())
		{
			return;
		}

		log.debug("Unpausing");

		for (DpsMember dpsMember : members.values())
		{
			dpsMember.unpause();
		}
		total.unpause();
	}
}
