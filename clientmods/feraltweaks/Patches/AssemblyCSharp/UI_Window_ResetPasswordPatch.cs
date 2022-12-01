using BepInEx;
using BepInEx.Logging;
using HarmonyLib;
using System.Collections.Generic;
using System.IO;
using System.Text.RegularExpressions;

namespace feraltweaks.Patches.AssemblyCSharp
{
    public class UI_Window_ResetPasswordPatch
    {
        [HarmonyPrefix]
        [HarmonyPatch(typeof(UI_Window_ResetPassword), "Setup")]
        public static void Setup(ref UI_Window_ResetPassword __instance)
        {
            if (__instance._emailInput == null)
            {
                return;
            }
            ManualLogSource logger = Plugin.logger;

            Dictionary<string, string> PatchConfig = new Dictionary<string, string>();
            logger.LogInfo("Patching password reset window...");
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

            // Check AllowNonEmailUsernames
            if (PatchConfig.GetValueOrDefault("AllowNonEmailUsernames", "false").ToLower() == "true")
            {
                __instance._emailInput.contentType = TMPro.TMP_InputField.ContentType.Standard;
                __instance._emailInput.characterValidation = TMPro.TMP_InputField.CharacterValidation.None;
                if (PatchConfig.ContainsKey("UserNameMaxLength"))
                    __instance._emailInput.characterLimit = int.Parse(PatchConfig["UserNameMaxLength"]);
            }
        }

        [HarmonyPrefix]
        [HarmonyPatch(typeof(UI_Window_ResetPassword), "IsValidEmail")]
        public static bool IsValidEmail(ref UI_Window_ResetPassword __instance, ref bool __result)
        {
            Dictionary<string, string> PatchConfig = new Dictionary<string, string>();
            foreach (string line in File.ReadAllLines(Paths.ConfigPath + "/feraltweaks/settings.props"))
            {
                if (line == "" || line.StartsWith("#") || !line.Contains("="))
                    continue;
                string key = line.Remove(line.IndexOf("="));
                string value = line.Substring(line.IndexOf("=") + 1);
                PatchConfig[key] = value;
            }
            if (!PatchConfig.ContainsKey("UserNameRegex"))
                return true;

            __result = Regex.Match(__instance.Email, PatchConfig["UserNameRegex"]).Success;
            return false;
        }

        [HarmonyPrefix]
        [HarmonyPatch(typeof(UI_Window_ResetPassword), "OnEmailChanged")]
        public static bool OnEmailChanged(ref UI_Window_ResetPassword __instance)
        {
            Dictionary<string, string> PatchConfig = new Dictionary<string, string>();
            foreach (string line in File.ReadAllLines(Paths.ConfigPath + "/feraltweaks/settings.props"))
            {
                if (line == "" || line.StartsWith("#") || !line.Contains("="))
                    continue;
                string key = line.Remove(line.IndexOf("="));
                string value = line.Substring(line.IndexOf("=") + 1);
                PatchConfig[key] = value;
            }
            if (!PatchConfig.ContainsKey("UserNameRegex"))
                return true;

            __instance._resetBtn.interactable = Regex.Match(__instance.Email, PatchConfig["UserNameRegex"]).Success;
            return false;
        }
    }
}
