/*
 * Copyright (c) 2018, Nickolaj <https://github.com/fire-proof>
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
package net.runelite.client.plugins.nightmarezone;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.util.QuantityFormatter;

class NightmareZoneOverlay extends OverlayPanel
{
	private final Client client;
	private final NightmareZoneConfig config;
	private final NightmareZonePlugin plugin;

	@Inject
	NightmareZoneOverlay(
			Client client,
			NightmareZoneConfig config,
			NightmareZonePlugin plugin)
	{
		super(plugin);
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(OverlayPriority.LOW);
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "NMZ overlay"));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isInNightmareZone() || !config.moveOverlay())
		{
			// Restore original widget
			Widget nmzWidget = client.getWidget(WidgetInfo.NIGHTMARE_ZONE);
			if (nmzWidget != null)
			{
				nmzWidget.setHidden(false);
			}
			return null;
		}

		Widget nmzWidget = client.getWidget(WidgetInfo.NIGHTMARE_ZONE);

		if (nmzWidget != null)
		{
			nmzWidget.setHidden(true);
		}

		final int currentPoints = client.getVar(Varbits.NMZ_POINTS);
		final int totalPoints = currentPoints + client.getVar(VarPlayer.NMZ_REWARD_POINTS);

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Points: ")
			.right(QuantityFormatter.formatNumber(currentPoints))
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Points/Hr: ")
			.right(QuantityFormatter.formatNumber(plugin.getPointsPerHour()))
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Total: ")
			.right(QuantityFormatter.formatNumber(totalPoints))
			.build());

		return super.render(graphics);
	}
}
