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

namespace org.asf.emuferal.client.plugin.patches
{
    [HarmonyPatch(typeof(GlobalSettingsManager))]
    public class GlobalSettingsManagerPatches
    {

        public static string devBaseURL;
        public static string stageBaseURL;
        public static string prodBaseURL;
        public static string devSharedBaseURL;
        public static string stageSharedBaseURL;
        public static string sharedBaseURL;
        public static bool debug;

        public static ManualLogSource logger;

        [HarmonyPrepare]
        public static void Initializer()
        {
            var config = new ConfigFile(Path.Combine(Paths.ConfigPath, "org.asf.emuferal.client.plugin.cfg"), true);

            devBaseURL = config.Bind("Servers",
                "DevBaseURL",
                "https://content.dev.wildworks.systems/Feral/",
                "DevBaseURL").Value;

            stageBaseURL = config.Bind("Servers",
                "StageBaseURL",
                "https://stage-game-assets.fer.al/",
                "StageBaseURL").Value;

            prodBaseURL = config.Bind("Servers",
                "ProdBaseURL",
                "https://game-assets.fer.al/",
                "ProdBaseURL").Value;

            devSharedBaseURL = config.Bind("Servers",
                "DevSharedBaseURL",
                "http://dev-s2.fer.al/",
                "DevSharedBaseURL").Value;

            stageSharedBaseURL = config.Bind("Servers",
                "StageSharedBaseURL",
                "https://stage-s2.fer.al/",
                "StageSharedBaseURL").Value;

            sharedBaseURL = config.Bind("Servers",
                "SharedBaseURL",
                "https://s2.fer.al/",
                "SharedBaseURL").Value;

            var debugStr = config.Bind("Developer",
                "Debug",
                "False",
                "Enables debugging output to the bepinex console.").Value;

            debug = bool.Parse(debugStr);
            logger = Plugin.logger;
        }

        [HarmonyPostfix]
        [HarmonyPatch(nameof(GlobalSettingsManager.DevBaseURL))]
        [HarmonyPatch(MethodType.Getter)]
        public static void DevBaseURL_PostFixPatch(ref string __result)
        {
            __result = devBaseURL;

            if (debug)
                logger.LogInfo($"Called DevBaseURL_PostFixPatch, returning {devBaseURL} ...");
        }

        [HarmonyPostfix]
        [HarmonyPatch(nameof(GlobalSettingsManager.StageBaseURL))]
        [HarmonyPatch(MethodType.Getter)]
        public static void StageBaseURL_PostFixPatch(ref string __result)
        {
            __result = stageBaseURL;

            if (debug)
                logger.LogInfo($"Called StageBaseURL_PostFixPatch, returning {stageBaseURL} ...");
        }

        [HarmonyPostfix]
        [HarmonyPatch(nameof(GlobalSettingsManager.ProdBaseURL))]
        [HarmonyPatch(MethodType.Getter)]
        public static void ProdBaseURL_PostFixPatch(ref string __result)
        {
            __result = prodBaseURL;

            if (debug)
                logger.LogInfo($"Called ProdBaseURL_PostFixPatch, returning {prodBaseURL} ...");
        }

        [HarmonyPostfix]
        [HarmonyPatch(nameof(GlobalSettingsManager.DevSharedBaseURL))]
        [HarmonyPatch(MethodType.Getter)]
        public static void DevSharedBaseURL_PostFixPatch(ref string __result)
        {
            __result = devSharedBaseURL;

            if (debug)
                logger.LogInfo($"Called ProdBaseURL_PostFixPatch, returning {devSharedBaseURL} ...");
        }


        [HarmonyPostfix]
        [HarmonyPatch(nameof(GlobalSettingsManager.StageSharedBaseURL))]
        [HarmonyPatch(MethodType.Getter)]
        public static void StageSharedBaseURL_PostFixPatch(ref string __result)
        {
            __result = stageSharedBaseURL;

            if (debug)
                logger.LogInfo($"Called ProdBaseURL_PostFixPatch, returning {stageSharedBaseURL} ...");
        }

        [HarmonyPostfix]
        [HarmonyPatch(nameof(GlobalSettingsManager.SharedBaseURL))]
        [HarmonyPatch(MethodType.Getter)]
        public static void SharedBaseURL_PostFixPatch(ref string __result)
        {
            __result = sharedBaseURL;

            if (debug)
                logger.LogInfo($"Called ProdBaseURL_PostFixPatch, returning {sharedBaseURL} ...");
        }
    }
}
