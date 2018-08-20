package net.runelite.client.ping;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;
import java.util.Arrays;
import java.util.List;

public class IcmpEchoReply extends Structure
{
	public static final int SIZE = 40;

	public WinDef.ULONG Address;
	public WinDef.ULONG Status;
	public WinDef.ULONG RoundTripTime;
	public WinDef.USHORT DataSize;
	public WinDef.USHORT Reserved;
	public WinDef.PVOID Data;
	public WinDef.UCHAR Ttl;
	public WinDef.UCHAR Tos;
	public WinDef.UCHAR Flags;
	public WinDef.UCHAR OptionsSize;
	public WinDef.PVOID OptionsData;

	public IcmpEchoReply(Pointer p)
	{
		super(p);
	}

	@Override
	protected List<String> getFieldOrder()
	{
		return Arrays.asList("Address", "Status", "RoundTripTime", "DataSize", "Reserved", "Data", "Ttl", "Tos", "Flags", "OptionsSize", "OptionsData");
	}
}
