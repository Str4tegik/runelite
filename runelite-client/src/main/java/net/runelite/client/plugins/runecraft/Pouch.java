/*
 * Copyright (c) 2019 Hydrox6 <ikada@protonmail.ch>
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
package net.runelite.client.plugins.runecraft;

import lombok.Data;

@Data
class Pouch
{
	private final int tier;

	private final int itemId;
	private int holdAmount;
	private final int baseHoldAmount;

	private boolean degraded;
	private final int degradedItemId;
	private final int degradedBaseHoldAmount;

	private int holding;
	boolean unknown = true;

	/*
			.put(ItemID.SMALL_POUCH, new Pouch(0, ItemID.SMALL_POUCH, 3))
		.put(ItemID.MEDIUM_POUCH, new Pouch(1, ItemID.MEDIUM_POUCH, 6, ItemID.MEDIUM_POUCH_5511, 3))
		.put(ItemID.LARGE_POUCH, new Pouch(2, ItemID.LARGE_POUCH, 9, ItemID.LARGE_POUCH_5513, 7))
		.put(ItemID.GIANT_POUCH, new Pouch(3, ItemID.GIANT_POUCH, 12, ItemID.GIANT_POUCH_5515, 9))
		.build();
	 */

	Pouch(int tier, int itemId, int holdAmount)
	{
		this(tier, itemId, holdAmount, -1, -1);
	}

	Pouch(int tier, int itemId, int holdAmount, int degradedId, int degradedHoldAmount)
	{
		this.tier = tier;
		this.itemId = itemId;
		this.holdAmount = holdAmount;
		this.baseHoldAmount = holdAmount;
		this.degradedBaseHoldAmount = degradedHoldAmount;
		this.degradedItemId = degradedId;

		this.holding = 0;
		this.degraded = false;
	}

	int getRemaining()
	{
		return holdAmount - holding;
	}

	void addHolding(int delta)
	{
		holding += delta;
		if (holding < 0)
		{
			holding = 0;
		}
		if (holding > holdAmount)
		{
			holding = holdAmount;
		}
	}

	void shouldDegradeStateChange(int itemId)
	{
		final boolean state = itemId == degradedItemId;
		if (state != degraded)
		{
			degraded = state;
			holdAmount = state ? degradedBaseHoldAmount : baseHoldAmount;
			holding = Math.min(holding, holdAmount);
		}
	}
}
