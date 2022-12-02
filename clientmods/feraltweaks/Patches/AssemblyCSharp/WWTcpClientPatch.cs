using HarmonyLib;
using System;
using System.IO;
using System.Text;

namespace feraltweaks.Patches.AssemblyCSharp
{
    public class WWTcpClientPatch
    {
        [HarmonyPrefix]
        [HarmonyPatch(typeof(WWTcpClient), "HandleSocketData")]
        public static bool HandleSocketData(ref WWTcpClient __instance)
        {
            // Lets redo ww's fucking nightmare and do it right
            while (__instance.connected)
            {
                try
                {
                    MemoryStream packetBuffer = new MemoryStream();
                    while (__instance.connected)
                    {
                        int b = __instance.Stream.ReadByte();
                        if (b == -1)
                            throw new IOException("Stream closed");
                        if (b == 0)
                        {
                            // Packet fully read
                            byte[] pkt = packetBuffer.ToArray();
                            packetBuffer = new MemoryStream();
                            __instance.HandleMessage(Encoding.UTF8.GetString(pkt));
                        }
                        else
                            packetBuffer.WriteByte((byte)b);
                    }
                } 
                catch (Exception e)
                {
                    __instance.DebugMessage("Disconnecting, exception caught: " + e);
                    __instance.Disconnect();
                    break;
                }
            }
            return false;
        }
    }
}
