using BepInEx;
using BepInEx.Configuration;
using BepInEx.IL2CPP;
using BepInEx.Logging;
using HarmonyLib;
using HarmonyLib.Tools;
using feraltweaks.Patches.AssemblyCSharp;
using System;
using System.Collections.Generic;
using System.IO;
using System.Threading;
using System.Threading.Tasks;

namespace feraltweaks
{
    [BepInProcess("Fer.al.exe")]
    [BepInProcess("Feral.exe")]
    [BepInPlugin(PluginInfo.PLUGIN_GUID, PluginInfo.PLUGIN_NAME, PluginInfo.PLUGIN_VERSION)]
    public class Plugin : BasePlugin
    {
        public static ManualLogSource logger;

        public override void Load()
        {
            logger = Log;

            // Patch with harmony
            Log.LogInfo("Applying patches...");
            Harmony.CreateAndPatchAll(typeof(BaseDefPatch));
            Harmony.CreateAndPatchAll(typeof(CoreChartDataManagerPatch));
            Harmony.CreateAndPatchAll(typeof(UI_Window_AccountCreationPatch));
            Harmony.CreateAndPatchAll(typeof(UI_Window_ChangeDisplayNamePatch));
            Harmony.CreateAndPatchAll(typeof(UI_Window_ResetPasswordPatch));
            Harmony.CreateAndPatchAll(typeof(UI_Window_TradeItemQuantityPatch));
            Harmony.CreateAndPatchAll(typeof(WWTcpClientPatch));
            Harmony.CreateAndPatchAll(typeof(WindUpdraftPatch));
            Harmony.CreateAndPatchAll(typeof(LoginLogoutPatches));
            Harmony.CreateAndPatchAll(typeof(CoreBundleManager2Patch));
            Harmony.CreateAndPatchAll(typeof(WorldObjectManagerPatch));
        }

        public static void WriteDefaultConfig()
        {
            File.WriteAllText(Paths.ConfigPath + "/feraltweaks/settings.props", "DisableUpdraftAudioSuppressor=false\nAllowNonEmailUsernames=false\nFlexibleDisplayNames=false\nUserNameRegex=^[\\w%+\\.-]+@(?:[a-zA-Z0-9-]+[\\.{1}])+[a-zA-Z]{2,}$\nDisplayNameRegex=^[0-9A-Za-z\\-_. ]+\nUserNameMaxLength=320\nDisplayNameMaxLength=16\nTradeItemLimit=99\n");
        }
    }
}
