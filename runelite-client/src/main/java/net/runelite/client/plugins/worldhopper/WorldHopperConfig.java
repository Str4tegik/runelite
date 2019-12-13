/*
 * Copyright (c) 2018, Lotto <https://github.com/devLotto>
 * Copyright (c) 2019, gregg1494 <https://github.com/gregg1494>
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
package net.runelite.client.plugins.worldhopper;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup(WorldHopperConfig.GROUP)
public interface WorldHopperConfig extends Config
{
	String GROUP = "worldhopper";

	@ConfigItem(
		keyName = "previousKey",
		name = "Quick-hop previous",
		description = "When you press this key you'll hop to the previous world",
		position = 0
	)
	default Keybind previousKey()
	{
		return new Keybind(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	}

	@ConfigItem(
		keyName = "nextKey",
		name = "Quick-hop next",
		description = "When you press this key you'll hop to the next world",
		position = 1
	)
	default Keybind nextKey()
	{
		return new Keybind(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	}

	@ConfigItem(
		keyName = "quickhopOutOfDanger",
		name = "Quick-hop out of dangerous worlds",
		description = "Don't hop to a PVP/high risk world when quick-hopping",
		position = 2
	)
	default boolean quickhopOutOfDanger()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSidebar",
		name = "Show world hopper sidebar",
		description = "Show sidebar containing all worlds that mimics in-game interface",
		position = 3
	)
	default boolean showSidebar()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ping",
		name = "Show world ping",
		description = "Shows ping to each game world",
		position = 4
	)
	default boolean ping()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMessage",
		name = "Show world hop message in chat",
		description = "Shows what world is being hopped to in the chat",
		position = 5
	)
	default boolean showWorldHopMessage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "menuOption",
		name = "Show Hop-to menu option",
		description = "Adds Hop-to menu option to the friends list and clan members list",
		position = 6
	)
	default boolean menuOption()
	{
		return true;
	}

	@ConfigItem(
		keyName = "subscriptionFilter",
		name = "Show subscription types",
		description = "Only show free worlds, member worlds, or both types of worlds in sidebar",
		position = 7
	)
	default SubscriptionFilterMode subscriptionFilter()
	{
		return SubscriptionFilterMode.BOTH;
	}

	@ConfigItem(
		keyName = "showUnrestrictedWorlds",
		name = "Show Unrestricted",
		description = "Show unrestricted worlds",
		position = 8
	)
	default boolean showUnrestrictedWorlds()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPvpWorlds",
		name = "Show PVP",
		description = "Show PVP worlds",
		position = 9
	)
	default boolean showPvpWorlds()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBountyHunterWorlds",
		name = "Show Bounty Hunter",
		description = "Show bounty hunter worlds",
		position = 10
	)
	default boolean showBountyHunterWorlds()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSkillTotalWorlds",
		name = "Show Skill Total",
		description = "Show skill total worlds",
		position = 11
	)
	default boolean showSkillTotalWorlds()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showHighRiskWorld",
		name = "Show High Risk",
		description = "Show high risk worlds",
		position = 12
	)
	default boolean showHighRiskWorld()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showTournamentWorlds",
		name = "Show Tournament",
		description = "Show Tournament worlds",
		position = 13
	)
	default boolean showTournamentWorlds()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDeadmanWorlds",
		name = "Show Deadman",
		description = "Show Deadman worlds",
		position = 14
	)
	default boolean showDeadmanWorlds()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showLeagueWorlds",
		name = "Show League",
		description = "Show League worlds",
		position = 15
	)
	default boolean showLeagueWorlds()
	{
		return true;
	}

	@ConfigItem(
		keyName = "displayPing",
		name = "Display current ping",
		description = "Displays ping to current game world",
		position = 16
	)
	default boolean displayPing()
	{
		return false;
	}
}
