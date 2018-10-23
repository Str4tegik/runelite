package net.runelite.client.plugins.loottracker;

import java.awt.color.ColorSpace;
import javax.swing.JFrame;
import net.runelite.api.ItemID;
import net.runelite.client.game.AsyncBufferedImage;
import net.runelite.client.game.ItemManager;
import org.junit.Test;
import org.mockito.Matchers;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LootTrackerPanelTest
{
	@Test
	public void test() throws InterruptedException
	{
		ItemManager itemManager = mock(ItemManager.class);
		when(itemManager.getImage(Matchers.anyInt(), Matchers.anyInt(), Matchers.anyBoolean()))
			.thenReturn(new AsyncBufferedImage(42,42, ColorSpace.TYPE_RGB));
		LootTrackerPanel panel = new LootTrackerPanel(
			mock(LootTrackerPlugin.class),
			itemManager
		);
		for (int i = 0; i < 1000; ++i) {
			LootTrackerItem lootTrackerItem = new LootTrackerItem(
				ItemID.ABYSSAL_WHIP,
			"Abyssal whip",
			1,
			42, false);
			panel.add("test" + i, 1337, new LootTrackerItem[] { lootTrackerItem });
		}
		//panel.setVisible(true);
		JFrame frame = new JFrame();
		//frame.add(panel);
		frame.add(panel.getWrappedPanel());
		frame.pack();
		frame.setVisible(true);
		while(true) {
			Thread.sleep(1000L);
		}
	}
}