using BepInEx;
using BepInEx.Logging;
using HarmonyLib;
using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using UnityEngine;

namespace feraltweaks.Patches.AssemblyCSharp
{
    public class WindUpdraftPatch
    {
        [HarmonyPostfix]
        [HarmonyPatch(typeof(WindUpdraft), "MStart")]
        public static void MStart(ref WindUpdraft __instance)
        {
            Dictionary<string, string> PatchConfig = new Dictionary<string, string>();
            ManualLogSource logger = Plugin.logger;

            Directory.CreateDirectory(Paths.ConfigPath + "/feraltweaks");
            if (!File.Exists(Paths.ConfigPath + "/feraltweaks/settings.props"))
            {
                Plugin.WriteDefaultConfig();
            }
            else
            {
                foreach (string line in File.ReadAllLines(Paths.ConfigPath + "/feraltweaks/settings.props"))
                {
                    if (line == "" || line.StartsWith("#") || !line.Contains("="))
                        continue;
                    string key = line.Remove(line.IndexOf("="));
                    string value = line.Substring(line.IndexOf("=") + 1);
                    PatchConfig[key] = value;
                }
            }

            if (!PatchConfig.ContainsKey("DisableUpdraftAudioSuppressor") || PatchConfig["DisableUpdraftAudioSuppressor"].ToLower() == "false")
                return;
            logger.LogInfo("Patching wind updrafts...");
            Transform ch = __instance._updraftEnterExitAudioTrigger.gameObject.transform.parent.Find("updraft_rune_emitter");
            if (ch != null)
                ch.gameObject.SetActive(false);
        }
    }
}
