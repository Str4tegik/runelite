package net.runelite.client.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.runelite.client.ui.overlay.OverlayMenuEntry;

@Data
@AllArgsConstructor
public class OverlayMenuClicked
{
	OverlayMenuEntry entry;
}
