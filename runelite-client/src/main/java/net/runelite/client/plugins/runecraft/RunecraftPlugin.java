/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
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
package net.runelite.client.plugins.runecraft;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Provides;
import static java.lang.Math.min;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Runecraft",
	description = "Show minimap icons and clickboxes for abyssal rifts",
	tags = {"abyssal", "minimap", "overlay", "rifts", "rc", "runecrafting"}
)
@Slf4j
public class RunecraftPlugin extends Plugin
{
	private static final int INVENTORY_SIZE = 28;
	private static final String POUCH_DECAYED_NOTIFICATION_MESSAGE = "Your rune pouch has decayed.";
	private static final String POUCH_DECAYED_MESSAGE = "Your pouch has decayed through use.";
	private static final Pattern POUCH_CHECK_MESSAGE = Pattern.compile("^There (?:is|are) ([a-z]+)(?: pure)? essences? in this pouch\\.$");
	private static final ImmutableMap<String, Integer> TEXT_TO_NUMBER = ImmutableMap.<String, Integer>builder()
		.put("no", 0)
		.put("one", 1)
		.put("two", 2)
		.put("three", 3)
		.put("four", 4)
		.put("five", 5)
		.put("six", 6)
		.put("seven", 7)
		.put("eight", 8)
		.put("nine", 9)
		.put("ten", 10)
		.put("eleven", 11)
		.put("twelve", 12)
		.build();

	private final Deque<ClickOperation> clickedItems = new ArrayDeque<>();
	private final Deque<ClickOperation> checkedPouches = new ArrayDeque<>();
	private int lastEssence;
	private int lastSpace;

	@Getter(AccessLevel.PACKAGE)
	private final Set<DecorativeObject> abyssObjects = new HashSet<>();

	@Getter(AccessLevel.PACKAGE)
	private NPC darkMage;

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private AbyssOverlay abyssOverlay;

	@Inject
	private EssencePouchOverlay essencePouchOverlay;

	@Inject
	private RunecraftConfig config;

	@Inject
	private Notifier notifier;

	@Provides
	RunecraftConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RunecraftConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(abyssOverlay);
		overlayManager.add(essencePouchOverlay);
		abyssOverlay.updateConfig();

		// Reset pouch state
		for (Pouch pouch : Pouch.values())
		{
			pouch.setHolding(0);
			pouch.setUnknown(true);
			pouch.degrade(false);
		}

		lastEssence = lastSpace = -1;
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(abyssOverlay);
		overlayManager.remove(essencePouchOverlay);
		abyssObjects.clear();
		darkMage = null;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("runecraft"))
		{
			abyssOverlay.updateConfig();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		if (config.degradingNotification())
		{
			if (event.getMessage().contains(POUCH_DECAYED_MESSAGE))
			{
				notifier.notify(POUCH_DECAYED_NOTIFICATION_MESSAGE);
			}
		}
		if (!checkedPouches.isEmpty())
		{
			Matcher matcher = POUCH_CHECK_MESSAGE.matcher(event.getMessage());
			if (matcher.matches())
			{
				final int num = TEXT_TO_NUMBER.get(matcher.group(1));
				// Keep getting operations until we get a valid one
				do
				{
					final ClickOperation op = checkedPouches.pop();
					if (op.tick >= client.getTickCount())
					{
						Pouch pouch = op.pouch;
						pouch.setHolding(num);
						pouch.setUnknown(false);
						break;
					}
				}
				while (!checkedPouches.isEmpty());
			}
		}
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		DecorativeObject decorativeObject = event.getDecorativeObject();
		if (AbyssRifts.getRift(decorativeObject.getId()) != null)
		{
			abyssObjects.add(decorativeObject);
		}
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		DecorativeObject decorativeObject = event.getDecorativeObject();
		abyssObjects.remove(decorativeObject);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();
		switch (gameState)
		{
			case LOADING:
				abyssObjects.clear();
				break;
			case CONNECTION_LOST:
			case HOPPING:
			case LOGIN_SCREEN:
				darkMage = null;
				break;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (InventoryID.INVENTORY.getId() != event.getContainerId())
		{
			return;
		}

		final Item[] items = event.getItemContainer().getItems();

		int newEss = 0;
		int newSpace = 0;
		Pouch medium = Pouch.MEDIUM;
		Pouch large = Pouch.LARGE;
		Pouch giant = Pouch.GIANT;

		// Count ess/space, and change pouch states
		for (Item item : items)
		{
			switch (item.getId())
			{
				case ItemID.PURE_ESSENCE:
					newEss += 1;
					break;
				case -1:
					newSpace += 1;
					break;
				case ItemID.MEDIUM_POUCH:
				case ItemID.LARGE_POUCH:
				case ItemID.GIANT_POUCH:
					Pouch pouch = Pouch.forItem(item.getId());
					pouch.degrade(false);
					break;
				case ItemID.MEDIUM_POUCH_5511:
				case ItemID.LARGE_POUCH_5513:
				case ItemID.GIANT_POUCH_5515:
					pouch = Pouch.forItem(item.getId());
					pouch.degrade(true);
					break;
			}
		}
		if (items.length < INVENTORY_SIZE)
		{
			newSpace += INVENTORY_SIZE - items.length;
		}

		if (clickedItems.isEmpty())
		{
			lastSpace = newSpace;
			lastEssence = newEss;
			return;
		}

		if (lastEssence == -1 || lastSpace == -1)
		{
			lastSpace = newSpace;
			lastEssence = newEss;
			clickedItems.clear();
			return;
		}

		final int tick = client.getTickCount();

		int essence = lastEssence;
		int space = lastSpace;

		log.debug("Begin processing {} events, last ess: {} space: {}, cur ess {}: space {}", clickedItems.size(), lastEssence, lastSpace, newEss, newSpace);

		while (essence != newEss)
		{
			ClickOperation op = clickedItems.poll();
			if (op == null)
				break;

			if (tick > op.tick)
			{
				continue;
			}

			Pouch pouch = op.pouch;

			final boolean fill = op.delta > 0;
			// How much ess can either be deposited or withdrawn
			final int required = fill ? pouch.getRemaining() : pouch.getHolding();
			// Bound to how much ess or free space we actually have, and optionally negate
			final int essenceGot = op.delta * min(required, fill ? essence : space);

			// if we have enough essence or space to fill or empty the entire pouch, it no
			// longer becomes unknown
			if (pouch.isUnknown() && (fill ? essence : space) >= pouch.getHoldAmount())
			{
				pouch.setUnknown(false);
			}

			log.debug("{}: {}", pouch.name(), essenceGot);

			essence -= essenceGot;
			space += essenceGot;

			pouch.addHolding(essenceGot);
		}

		log.debug("End processing with {} events left", clickedItems.size());
//		clickedItems.clear();

		lastSpace = newSpace;
		lastEssence = newEss;
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		switch (event.getMenuAction())
		{
			case ITEM_FIRST_OPTION:
			case ITEM_SECOND_OPTION:
			case ITEM_THIRD_OPTION:
			case ITEM_FOURTH_OPTION:
			case ITEM_FIFTH_OPTION:
			case GROUND_ITEM_THIRD_OPTION: // Take
				break;
			default:
				return;
		}

		final int id = event.getId();
		final Pouch pouch = Pouch.forItem(id);
		if (pouch == null)
		{
			return;
		}

		final int tick = client.getTickCount() + 3;
		switch (event.getMenuOption())
		{
			case "Fill":
				clickedItems.add(new ClickOperation(pouch, tick, 1));
				break;
			case "Empty":
				clickedItems.add(new ClickOperation(pouch, tick, -1));
				break;
			case "Check":
				checkedPouches.add(new ClickOperation(pouch, tick));
				break;
			case "Take":
				// Dropping pouches clears them, so clear when picked up
				pouch.setHolding(0);
				break;
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		final NPC npc = event.getNpc();
		if (npc.getId() == NpcID.DARK_MAGE)
		{
			darkMage = npc;
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		final NPC npc = event.getNpc();
		if (npc == darkMage)
		{
			darkMage = null;
		}
	}
}
