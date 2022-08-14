using System;
using BepInEx;
using BepInEx.Configuration;
using BepInEx.Logging;
using BepInEx.Preloader.Core.Patching;

namespace org.asf.centuria.client.patcher 
{
    [PatcherPluginInfo(PLUGIN_GUID, PLUGIN_GUID, "1.0.0.0")]
    [BepInProcess("Fer.al.exe")]
    public class Patcher : BasePatcher
    {
        public const string PLUGIN_GUID = "org.asf.centuria.client.patcher";

        internal static ConfigFile configuration;
        internal static ManualLogSource logger;

        public override void Initialize() 
        {
            logger = Log;

            // Plugin startup logic
            logger.LogInfo($"Plugin " + PLUGIN_GUID + " is loaded!");

            //load config
            logger.LogInfo(PLUGIN_GUID + ": Loading configuration options...");
            configuration = Config;

            //Nothing here yet..

        }

        public void ServerEnvironment_isDebug_patch()
        {

        }
    }
}
