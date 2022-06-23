using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using BepInEx;
using BepInEx.Configuration;
using BepInEx.Logging;
using HarmonyLib;

namespace org.asf.emuferal.client.plugin.Patches.AssemblyCSharp
{
    [HarmonyPatch(typeof(ServerEnvironment))]
    public class ServerEnvironmentPatches
    {

        public static bool devMode;
        public static bool debug;

        public static ManualLogSource logger;

        [HarmonyPrepare]
        public static void Initializer()
        {
            var config = new ConfigFile(Path.Combine(Paths.ConfigPath, "org.asf.emuferal.client.plugin.cfg"), true);

            var devModeStr = config.Bind("Developer",
                "DevMode",
                "false",
                "Enables / disables various developer tools in the fer.al client.").Value;

            var debugStr = config.Bind("Developer",
                "DebugPluginLogging",
                "false",
                "Enables debugging output to the bepinex console.").Value;

            devMode = bool.Parse(devModeStr);
            debug = bool.Parse(debugStr);

            logger = Plugin.logger;
        }

    }
}
