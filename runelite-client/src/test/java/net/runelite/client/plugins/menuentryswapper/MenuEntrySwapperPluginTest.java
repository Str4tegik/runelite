package net.runelite.client.plugins.menuentryswapper;

import com.google.inject.Guice;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MenuEntrySwapperPluginTest
{
	private static final String TALK_TO = "Talk-to";

	@Inject
	private MenuEntrySwapperPlugin menuEntrySwapperPlugin;

	@Mock
	@Bind
	private MenuEntrySwapperConfig config;

	@Mock
	@Bind
	private Client client;

	@Mock
	@Bind
	private ScheduledExecutorService scheduledExecutorService;

	@Before
	public void before()
	{
		Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);

		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
	}

	@Test
	public void testPickpocket()
	{
		when(config.swapPickpocket()).thenReturn(true);
		testSwap("H.A.M. Member", TALK_TO, "Pickpocket");
	}

	private void testSwap(String target, String opt1, String opt2)
	{
		MenuEntry[] entries = new MenuEntry[2];

		MenuEntry entry1 = new MenuEntry();
		entry1.setTarget(target);
		entry1.setOption(opt1);
		entries[0] = entry1;

		MenuEntry entry2 = new MenuEntry();
		entry2.setTarget(target);
		entry2.setOption(opt2);
		entries[1] = entry2;

		when(client.getMenuEntries()).thenReturn(entries);

		MenuEntryAdded menuEntryAdded = new MenuEntryAdded(opt1, target, -1, -1, -1, -1);
		menuEntrySwapperPlugin.onMenuEntryAdded(menuEntryAdded);

		ArgumentCaptor<MenuEntry[]> captor = ArgumentCaptor.forClass(MenuEntry[].class);
		verify(client).setMenuEntries(captor.capture());

		MenuEntry[] captorEntries = captor.getValue();
		assertEquals(2, captorEntries.length);
		assertEquals(entry2, captorEntries[0]);
		assertEquals(entry1, captorEntries[1]);
	}
}