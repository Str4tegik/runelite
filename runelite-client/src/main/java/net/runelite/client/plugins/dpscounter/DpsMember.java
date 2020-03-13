package net.runelite.client.plugins.dpscounter;

import java.time.Duration;
import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
class DpsMember
{
	private final String name;
	private Instant start;
	private Instant end;
	private int damage;

	void addDamage(int amount)
	{
		if (start == null)
		{
			start = Instant.now();
		}

		damage += amount;
	}

	float getDps()
	{
		if (start == null)
		{
			return 0;
		}

		Instant now = end == null ? Instant.now() : end;
		int diff = (int) (now.toEpochMilli() - start.toEpochMilli()) / 1000;
		if (diff == 0)
		{
			return 0;
		}

		return (float) damage / (float) diff;
	}

	void pause()
	{
		end = Instant.now();
	}

	boolean isPaused()
	{
		return start == null || end != null;
	}

	void unpause()
	{
		if (end == null)
		{
			return;
		}

		start = start.plus(Duration.between(end, Instant.now()));
		end = null;
	}

	void reset()
	{
		damage = 0;
		start = end = Instant.now();
	}

	Duration elapsed()
	{
		return Duration.between(start, end == null ? Instant.now() : end);
	}
}
