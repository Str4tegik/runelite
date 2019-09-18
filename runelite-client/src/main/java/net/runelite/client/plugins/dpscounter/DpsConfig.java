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
		return true;
	}
}
