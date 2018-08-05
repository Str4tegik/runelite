package net.runelite.client.ping;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface IPHlpAPI extends Library
{
	IPHlpAPI INSTANCE = Native.loadLibrary("IPHlpAPI", IPHlpAPI.class);

	long IcmpCreateFile();

	boolean IcmpCloseHandle(long handle);

	int IcmpSendEcho(long IcmpHandle, int DestinationAddress, Pointer RequestData, short RequestSize, long RequestOptions, IcmpEchoReply ReplyBuffer, int ReplySize, int Timeout);
}
