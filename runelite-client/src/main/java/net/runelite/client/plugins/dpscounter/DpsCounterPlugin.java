package net.runelite.client.plugins.dpscounter;

import com.google.inject.Inject;
import com.google.inject.Provides;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Hitsplat;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.api.events.ExperienceChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
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
import org.apache.commons.lang3.ArrayUtils;

@PluginDescriptor(
	name = "DPS Counter",
	description = "counts dps?"
)
@Slf4j
public class DpsCounterPlugin extends Plugin
{
	private static final Varbits[] TOB_PARTY_ORBS_VARBITS = new Varbits[]{
		Varbits.THEATRE_OF_BLOOD_ORB_1, Varbits.THEATRE_OF_BLOOD_ORB_2,
		Varbits.THEATRE_OF_BLOOD_ORB_3, Varbits.THEATRE_OF_BLOOD_ORB_4,
		Varbits.THEATRE_OF_BLOOD_ORB_5
	};

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

	private Boss boss;
	private NPC bossNpc;
	private int lastHpExp = -1;
	@Getter(AccessLevel.PACKAGE)
	private final Map<String, DpsMember> members = new ConcurrentHashMap<>();
	@Getter(AccessLevel.PACKAGE)
	private DpsMember total = new DpsMember("Total");

	@Provides
	DpsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DpsConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(dpsOverlay);
		wsClient.registerMessage(DpsUpdate.class);
	}

	@Override
	protected void shutDown()
	{
		wsClient.unregisterMessage(DpsUpdate.class);
		overlayManager.remove(dpsOverlay);
		members.clear();
		boss = null;
	}

	@Subscribe
	public void onPartyChanged(PartyChanged partyChanged)
	{
		members.clear();
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged interactingChanged)
	{
		Actor source = interactingChanged.getSource();
		Actor target = interactingChanged.getTarget();

		if (source != client.getLocalPlayer())
		{
			return;
		}

		if (target instanceof NPC)
		{
			NPC npc = (NPC) target;
			int npcId = npc.getId();
			Boss boss = Boss.findBoss(npcId);
			if (boss != null)
			{
				this.boss = boss;
				bossNpc = (NPC) target;
			}
		}
	}

	@Subscribe
	public void onExperienceChanged(ExperienceChanged experienceChanged)
	{
		if (experienceChanged.getSkill() != Skill.HITPOINTS)
		{
			return;
		}

		Player player = client.getLocalPlayer();
		final int xp = client.getSkillExperience(Skill.HITPOINTS);
		if (boss == null || lastHpExp < 0 || xp <= lastHpExp || bossNpc == null || bossNpc != player.getInteracting())
		{
			lastHpExp = xp;
			return;
		}

		final int delta = xp - lastHpExp;

		float modifier;
		if (Boss.isTOB(boss))
		{
			int partySize = getTobPartySize();
			System.out.println(partySize);
			modifier = boss.getModifier(partySize);
		}
		else
		{
			modifier = boss.getModifier();
		}

		final int hit = getHit(modifier, delta);
		lastHpExp = xp;

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

		if (hit > 0 && localMember != null)
		{
			final DpsUpdate specialCounterUpdate = new DpsUpdate(bossNpc.getId(), hit);
			specialCounterUpdate.setMemberId(partyService.getLocalMember().getMemberId());
			wsClient.send(specialCounterUpdate);
		}
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

		// Hmm - not attacking the same boss I am
		if (bossNpc == null || dpsUpdate.getNpcId() != bossNpc.getId())
		{
			return;
		}

		DpsMember dpsMember = members.computeIfAbsent(name, DpsMember::new);
		dpsMember.addDamage(dpsUpdate.getHit());

		if (dpsMember.isPaused())
		{
			dpsMember.unpause();
			log.debug("Unpausing {}", dpsMember.getName());
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied hitsplatApplied)
	{
		Actor actor = hitsplatApplied.getActor();
		Player local = client.getLocalPlayer();

		if (actor == local.getInteracting() || bossNpc == actor)
		{
			Hitsplat hitsplat = hitsplatApplied.getHitsplat();

			if (hitsplat.getHitsplatType() != Hitsplat.HitsplatType.DAMAGE)
			{
				return;
			}

			if (total.isPaused())
			{
				total.unpause();
			}

			total.addDamage(hitsplat.getAmount());
		}
	}

	@Subscribe
	public void onOverlayMenuClicked(OverlayMenuClicked event)
	{
		if (event.getEntry().getMenuAction() == MenuAction.RUNELITE_OVERLAY &&
			event.getEntry().getOption().equals("Reset") &&
			event.getEntry().getTarget().equals("DPS counter"))
		{
			members.clear();
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		if (boss == null)
		{
			return;
		}

		NPC npc = npcSpawned.getNpc();
		int npcId = npc.getId();
		if (!ArrayUtils.contains(boss.getIds(), npcId))
		{
			return;
		}

		log.debug("Boss has spawned!");
		bossNpc = npc;
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned)
	{
		if (bossNpc == null || npcDespawned.getNpc() != bossNpc)
		{
			return;
		}

		if (bossNpc.isDead())
		{
			log.debug("Boss has died!");
			pause();
		}

		bossNpc = null;
	}

	private void pause()
	{
		for (DpsMember dpsMember : members.values())
		{
			dpsMember.pause();
		}
		total.pause();
	}

	private int getHit(float modifier, int deltaExperience)
	{
		float modifierBase = 1f / modifier;
		float damageOutput = (deltaExperience * modifierBase) / 1.3333f;
		return Math.round(damageOutput);
	}

	private int getTobPartySize()
	{
		int partySize = 0;
		for (Varbits varbit : TOB_PARTY_ORBS_VARBITS)
		{
			if (client.getVar(varbit) != 0)
			{
				partySize++;
			}
			else
			{
				break;
			}
		}
		return partySize;
	}
}
