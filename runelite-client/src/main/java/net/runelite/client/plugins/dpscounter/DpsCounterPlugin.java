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

import com.google.common.collect.ImmutableSet;
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
import net.runelite.api.NpcID;
import net.runelite.api.Player;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcDespawned;
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

@PluginDescriptor(
	name = "DPS Counter",
	description = "Counts damage (per second) by a party",
	enabledByDefault = false
)
@Slf4j
public class DpsCounterPlugin extends Plugin
{
	private static final ImmutableSet<Integer> BOSSES = ImmutableSet.of(
		NpcID.ABYSSAL_SIRE, NpcID.ABYSSAL_SIRE_5887, NpcID.ABYSSAL_SIRE_5888, NpcID.ABYSSAL_SIRE_5889, NpcID.ABYSSAL_SIRE_5890, NpcID.ABYSSAL_SIRE_5891, NpcID.ABYSSAL_SIRE_5908,
		NpcID.CALLISTO, NpcID.CALLISTO_6609,
		NpcID.CERBERUS, NpcID.CERBERUS_5863, NpcID.CERBERUS_5866,
		NpcID.CHAOS_ELEMENTAL, NpcID.CHAOS_ELEMENTAL_6505,
		NpcID.CORPOREAL_BEAST,
		NpcID.GENERAL_GRAARDOR, NpcID.GENERAL_GRAARDOR_6494,
		NpcID.GIANT_MOLE, NpcID.GIANT_MOLE_6499,
		NpcID.KALPHITE_QUEEN, NpcID.KALPHITE_QUEEN_963, NpcID.KALPHITE_QUEEN_965, NpcID.KALPHITE_QUEEN_4303, NpcID.KALPHITE_QUEEN_4304, NpcID.KALPHITE_QUEEN_6500, NpcID.KALPHITE_QUEEN_6501,
		NpcID.KING_BLACK_DRAGON, NpcID.KING_BLACK_DRAGON_2642, NpcID.KING_BLACK_DRAGON_6502,
		NpcID.KRIL_TSUTSAROTH, NpcID.KRIL_TSUTSAROTH_6495,
		NpcID.VENENATIS, NpcID.VENENATIS_6610,
		NpcID.VETION, NpcID.VETION_REBORN,
		NpcID.THE_MAIDEN_OF_SUGADINTI, NpcID.THE_MAIDEN_OF_SUGADINTI_8361, NpcID.THE_MAIDEN_OF_SUGADINTI_8362, NpcID.THE_MAIDEN_OF_SUGADINTI_8363, NpcID.THE_MAIDEN_OF_SUGADINTI_8364, NpcID.THE_MAIDEN_OF_SUGADINTI_8365,
		NpcID.PESTILENT_BLOAT,
		NpcID.NYLOCAS_VASILIAS, NpcID.NYLOCAS_VASILIAS_8355, NpcID.NYLOCAS_VASILIAS_8356, NpcID.NYLOCAS_VASILIAS_8357,
		NpcID.SOTETSEG, NpcID.SOTETSEG_8388,
		NpcID.XARPUS_8340, NpcID.XARPUS_8341,
		NpcID.VERZIK_VITUR_8370,
		NpcID.VERZIK_VITUR_8372,
		NpcID.VERZIK_VITUR_8374
	);

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
	}

	@Subscribe
	public void onPartyChanged(PartyChanged partyChanged)
	{
		members.clear();
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied hitsplatApplied)
	{
		Player player = client.getLocalPlayer();
		Actor actor = hitsplatApplied.getActor();
		if (!(actor instanceof NPC))
		{
			return;
		}

		Hitsplat hitsplat = hitsplatApplied.getHitsplat();

		switch (hitsplat.getHitsplatType())
		{
			case DAMAGE_ME:
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
				final int npcId = ((NPC) actor).getId();
				boolean isBoss = BOSSES.contains(npcId);
				if (actor != player.getInteracting() && !isBoss)
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
	public void onNpcDespawned(NpcDespawned npcDespawned)
	{
		NPC npc = npcDespawned.getNpc();

		if (npc.isDead() && BOSSES.contains(npc.getId()))
		{
			log.debug("Boss has died!");

			if (dpsConfig.autopause())
			{
				pause();
			}
		}
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
