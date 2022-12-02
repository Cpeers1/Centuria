using BepInEx;
using BepInEx.Logging;
using HarmonyLib;
using System.Collections.Generic;
using System.IO;
using System.Text.RegularExpressions;

namespace feraltweaks.Patches.AssemblyCSharp
{
    public class UI_Window_TradeItemQuantityPatch
    {
        private static Dictionary<string, string> PatchConfig = new Dictionary<string, string>();

        [HarmonyPrepare]
        public static void Setup()
        {
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
        }

        [HarmonyPrefix]
        [HarmonyPatch(typeof(UI_Window_TradeItemQuantity), "ChosenQuantity")]
        [HarmonyPatch(MethodType.Setter)]
        public static bool ChosenQuantity_SET(ref UI_Window_TradeItemQuantity __instance, ref int value)
        {
            if (!PatchConfig.ContainsKey("TradeItemLimit"))
                return true;

            int limit = int.Parse(PatchConfig["TradeItemLimit"]);
            if (value < 0)
                value = 0;
            else if (value > limit)
                value = limit;
            __instance._chosenQuantity = value;
            __instance._inputField.SetTextWithoutNotify(value.ToString());
            __instance.RefreshQuantity();

            return false;
        }

        [HarmonyPrefix]
        [HarmonyPatch(typeof(UI_Window_TradeItemQuantity), "BtnClicked_Increase")]
        public static bool BtnClicked_Increase(ref UI_Window_TradeItemQuantity __instance)
        {
            if (!PatchConfig.ContainsKey("TradeItemLimit"))
                return true;

            int limit = int.Parse(PatchConfig["TradeItemLimit"]);
            int newQuantity = __instance._chosenQuantity + 1;
            if (newQuantity < 0)
                newQuantity = 0;
            else if (newQuantity > limit)
                newQuantity = limit;
            __instance._chosenQuantity = newQuantity;
            __instance._inputField.SetTextWithoutNotify(newQuantity.ToString());
            __instance.RefreshQuantity();

            return false;
        }

        [HarmonyPrefix]
        [HarmonyPatch(typeof(UI_Window_TradeItemQuantity), "BtnClicked_Decrease")]
        public static bool BtnClicked_Decrease(ref UI_Window_TradeItemQuantity __instance)
        {
            if (!PatchConfig.ContainsKey("TradeItemLimit"))
                return true;

            int limit = int.Parse(PatchConfig["TradeItemLimit"]);
            int newQuantity = __instance._chosenQuantity - 1;
            if (newQuantity < 0)
                newQuantity = 0;
            else if (newQuantity > limit)
                newQuantity = limit;
            __instance._chosenQuantity = newQuantity;
            __instance._inputField.SetTextWithoutNotify(newQuantity.ToString());
            __instance.RefreshQuantity();

            return false;
        }

    }
}
