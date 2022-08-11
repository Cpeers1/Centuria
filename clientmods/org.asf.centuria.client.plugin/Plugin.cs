using BepInEx;
using BepInEx.Configuration;
using BepInEx.IL2CPP;
using BepInEx.Logging;
using HarmonyLib;
using HarmonyLib.Tools;
using System;
using System.IO;

namespace org.asf.centuria.client.plugin
{
    [BepInPlugin(PluginInfo.PLUGIN_GUID, PluginInfo.PLUGIN_NAME, PluginInfo.PLUGIN_VERSION)]
    [BepInProcess("Fer.al.exe")]
    [BepInProcess("Feral.exe")]
    public class Plugin : BasePlugin
    {
        internal static ConfigFile configuration;
        internal static ManualLogSource logger;

        public override void Load()
        {
            logger = Log;

            // Plugin startup logic
            logger.LogInfo($"Plugin {PluginInfo.PLUGIN_GUID} is loaded!");

            //load config
            logger.LogInfo($"{PluginInfo.PLUGIN_GUID}: Loading configuration options...");
            configuration = Config;

            //harmony patch all..
            Log.LogInfo($"{PluginInfo.PLUGIN_GUID}: Patching methods using harmony...");
            Harmony harmonyInstance = new Harmony(PluginInfo.PLUGIN_GUID);
            HarmonyFileLog.Enabled = true;
            harmonyInstance.PatchAll();
        }
    }
}
