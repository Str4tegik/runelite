package net.runelite.client.plugins.dpscounter;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.text.DecimalFormat;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import net.runelite.api.Client;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY;
import net.runelite.api.Player;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.ws.PartyService;

class DpsOverlay extends Overlay
{
	private static final DecimalFormat DPS_FORMAT = new DecimalFormat("#0.0");

	private final DpsCounterPlugin dpsCounterPlugin;
	private final DpsConfig dpsConfig;
	private final PartyService partyService;
	private final Client client;
	private final boolean developerMode;

	private final PanelComponent panelComponent = new PanelComponent();

	@Inject
	DpsOverlay(DpsCounterPlugin dpsCounterPlugin, DpsConfig dpsConfig, PartyService partyService, Client client,
			   @Named("developerMode") boolean developerMode)
	{
		super(dpsCounterPlugin);
		this.dpsCounterPlugin = dpsCounterPlugin;
		this.dpsConfig = dpsConfig;
		this.partyService = partyService;
		this.client = client;
		this.developerMode = developerMode;
		getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY, "Reset", "DPS counter"));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Map<String, DpsMember> dpsMembers = dpsCounterPlugin.getMembers();
		if (dpsMembers.isEmpty())
		{
			return null;
		}

		boolean inParty = !partyService.getMembers().isEmpty();
		boolean showDamage = dpsConfig.showDamage();

		panelComponent.getChildren().clear();

		panelComponent.getChildren().add(
			TitleComponent.builder()
				.text(inParty ? "Party DPS" : "DPS")
				.build());

		for (DpsMember dpsMember : dpsMembers.values())
		{
			boolean paused = dpsMember.isPaused();
			panelComponent.getChildren().add(
				LineComponent.builder()
					.left(developerMode && paused ? "(P)" : dpsMember.getName())
					.right(showDamage ? Integer.toString(dpsMember.getDamage()) : DPS_FORMAT.format(dpsMember.getDps()))
					.build());
		}

		if (!inParty)
		{
			Player player = client.getLocalPlayer();
			if (player.getName() != null)
			{
				DpsMember total = dpsCounterPlugin.getTotal();
				DpsMember self = dpsMembers.get(player.getName());

				if (self != null && total.getDamage() > self.getDamage())
				{
					panelComponent.getChildren().add(
						LineComponent.builder()
							.left(total.getName())
							.right(showDamage ? Integer.toString(total.getDamage()) : DPS_FORMAT.format(total.getDps()))
							.build());
				}
			}
		}

		return panelComponent.render(graphics);
	}
}
