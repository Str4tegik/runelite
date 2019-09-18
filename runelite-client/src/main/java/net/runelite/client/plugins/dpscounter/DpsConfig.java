package net.runelite.client.plugins.dpscounter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("dpscounter")
public interface DpsConfig extends Config
{
	@ConfigItem(
		position = 0,
		keyName = "showDamage",
		name = "Show Damage",
		description = "Show total damage instead of DPS"
	)
	default boolean showDamage()
	{
		return false;
	}

	@ConfigItem(
		position = 1,
		keyName = "resetOnSpawn",
		name = "Reset on boss spawn",
		description = "Resets the damage counter whenever a new boss spawns"
	)
	default boolean resetOnSpawn()
	{
		return false;
	}
}
