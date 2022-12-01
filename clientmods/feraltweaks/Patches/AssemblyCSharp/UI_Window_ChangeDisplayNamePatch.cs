using BepInEx;
using BepInEx.Logging;
using HarmonyLib;
using System.Collections.Generic;
using System.IO;

namespace feraltweaks.Patches.AssemblyCSharp
{
    public class UI_Window_ChangeDisplayNamePatch
    {
        [HarmonyPrefix]
        [HarmonyPatch(typeof(UI_Window_ChangeDisplayName), "Setup")]
        public static void Setup(ref UI_Window_ChangeDisplayName __instance)
        {
            if (__instance._usernameInput == null)
            {
                return;
            }
            Dictionary<string, string> PatchConfig = new Dictionary<string, string>();
            ManualLogSource logger = Plugin.logger;

            logger.LogInfo("Patching display name change window...");
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

            // Check FlexibleDisplayNames
            if (PatchConfig.GetValueOrDefault("FlexibleDisplayNames", "false").ToLower() == "true")
            {
                __instance._usernameInput.contentType = TMPro.TMP_InputField.ContentType.Standard;
                __instance._usernameInput.characterValidation = TMPro.TMP_InputField.CharacterValidation.None;
                if (PatchConfig.ContainsKey("DisplayNameMaxLength"))
                    __instance._usernameInput.characterLimit = int.Parse(PatchConfig["DisplayNameMaxLength"]);
            }
        }
    }
}
