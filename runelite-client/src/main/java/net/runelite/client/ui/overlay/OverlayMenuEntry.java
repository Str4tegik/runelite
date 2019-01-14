package net.runelite.client.ui.overlay;

import lombok.Data;

@Data
public class OverlayMenuEntry
{
	public static final int MENU_ID_CONFIG = 1;

	private String option;
	private String target;
	private int identifier = -1;

//	private String configGroup;
//	private Config config;
//	private int param0;
//	private int param1;
}
