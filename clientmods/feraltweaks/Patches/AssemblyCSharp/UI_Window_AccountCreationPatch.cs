using BepInEx;
using BepInEx.Logging;
using HarmonyLib;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using UnityEngine.Networking;
using WW.Waiters;

namespace feraltweaks.Patches.AssemblyCSharp
{
    public class UI_Window_AccountCreationPatch
    {
        private static Dictionary<string, string> PatchConfig = new Dictionary<string, string>();
        private static RecreatedWaiter usernameVerifyWaiter;
        private class RecreatedWaiter
        {
            public long timestamp;
            public Action action;
        }

        [HarmonyPrefix]
        [HarmonyPatch(typeof(UI_Window_AccountCreation), "OnOpen")]
        public static void OnOpen(ref UI_Window_AccountCreation __instance)
        {
            ManualLogSource logger = Plugin.logger;

            logger.LogInfo("Patching account creation...");
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

            // Check FlexibleDisplayNames
            if (PatchConfig.GetValueOrDefault("FlexibleDisplayNames", "false").ToLower() == "true")
            {
                __instance._usernameInput.contentType = TMPro.TMP_InputField.ContentType.Standard;
                __instance._usernameInput.characterValidation = TMPro.TMP_InputField.CharacterValidation.None;
                if (PatchConfig.ContainsKey("DisplayNameMaxLength"))
                    __instance._usernameInput.characterLimit = int.Parse(PatchConfig["DisplayNameMaxLength"]);
            }
        }

        [HarmonyPrefix]
        [HarmonyPatch(typeof(UI_Window_AccountCreation), "RefreshUsernameStatus")]
        private static bool RefreshUsernameStatus(UI_Window_AccountCreation __instance)
        {
            if (!PatchConfig.ContainsKey("DisplayNameRegex"))
                return true;
            if (usernameVerifyWaiter != null)
                usernameVerifyWaiter = null;
            __instance._usernameStatus = RegisterUserStatus.ERROR_USERNAME_NOT_PRESENT;
            __instance.RefreshRegistrationButton();

            if (__instance.Username == "")
            {
                __instance._usernameStatusIndicator.SetStatus(UI_FieldStatusIndicator.FieldStatus.Empty, true);
                __instance._usernameErrorText.text = "";
                return false;
            }

            string user = __instance.Username;
            Action ac = () => {
                 __instance._usernameStatusIndicator.SetStatus(UI_FieldStatusIndicator.FieldStatus.Verifying, true);
                string status = RegisterUserStatus.SUCCESS;
                if (!Regex.Match(__instance.Username, PatchConfig["DisplayNameRegex"]).Success || user.EndsWith(" ") || user.StartsWith(" "))
                    status = RegisterUserStatus.ERROR_DISPLAY_NAME_INVALID_FORMAT;
                else if (__instance.Username.Length < 2)
                    status = RegisterUserStatus.ERROR_DISPLAY_NAME_TOO_SHORT;
                else if (__instance.Username.Length > int.Parse(PatchConfig.GetValueOrDefault("DisplayNameMaxLength", "16")))
                    status = RegisterUserStatus.ERROR_DISPLAY_NAME_TOO_LONG;
                else
                {
                    // Contact server
                    try
                    {
                        string nm = user;
                        nm = nm.Replace(" ", "%20");
                        nm = nm.Replace("/", "%2F");
                        nm = nm.Replace("\\", "%5C");
                        nm = nm.Replace("&", "%26");
                        nm = nm.Replace("=", "%3D");
                        string res = new HttpClient().PostAsync(ApiSrvHandler.Host + "/dn/validate/" + nm, new StringContent("")).GetAwaiter().GetResult().Content.ReadAsStringAsync().GetAwaiter().GetResult();
                        Dictionary<string, string> resp = new Dictionary<string, string>();

                        // Dirty parser for this json, very very basic
                        // Cannot use newtonsoft.json due to il2cpp
                        if (res != "")
                        {
                            string cKey = "";
                            string cValue = "";
                            bool isField = false;
                            bool isKey = true;
                            res = res.Substring(1);
                            res = res.Remove(res.LastIndexOf("}"));
                            foreach (char b in res)
                            {
                                if (b == '\"')
                                {
                                    if (isField)
                                    {
                                        isField = false;
                                        if (!isKey)
                                        {
                                            isKey = true;
                                            resp[cKey] = cValue;
                                            cKey = "";
                                            cValue = "";
                                        }
                                        else
                                        {
                                            isKey = false;
                                        }
                                    }
                                    else
                                    {
                                        isField = true;
                                    }
                                }
                                else
                                {
                                    if (isField)
                                    {
                                        if (isKey)
                                            cKey += b;
                                        else
                                            cValue += b;
                                    }
                                }
                            }
                        }

                        if (resp.ContainsKey("error") && resp["error"] != "")
                            status = resp["error"];
                    }
                    catch (Exception e) {
                        Plugin.logger.LogError("error " + e);
                    }
                }
                if (user == __instance.Username)
                {
                    __instance._usernameStatus = status;
                    __instance._usernameErrorText.text = __instance.IsValidUsername ? "" : RegisterUserResponse.GetLocalizedError(status);
                    __instance._usernameStatusIndicator.SetStatus(__instance.IsValidUsername ? UI_FieldStatusIndicator.FieldStatus.Valid : UI_FieldStatusIndicator.FieldStatus.Invalid);
                    __instance.RefreshRegistrationButton();
                }
            };
            usernameVerifyWaiter = new RecreatedWaiter() { 
                timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() + 1000,
                action = ac
            };

            return false;
        }

        [HarmonyPostfix]
        [HarmonyPatch(typeof(WaitController), "Update")]
        private static void Update(ref WaitController __instance )
        {
            RecreatedWaiter waiter = usernameVerifyWaiter;
            if (waiter != null && DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() >= waiter.timestamp)
            {
                waiter.action.Invoke();
                usernameVerifyWaiter = null;
            }
        }

        [HarmonyPrefix]
        [HarmonyPatch(typeof(UI_Window_AccountCreation), "CheckEmail")]
        private static bool CheckEmail(ref UI_Window_AccountCreation __instance, ref string __result)
        {
            if (!PatchConfig.ContainsKey("UserNameRegex"))
                return true;
            if (__instance._cachedEmailValidations.ContainsKey(__instance.Email))
            {
                __result = __instance._cachedEmailValidations[__instance.Email];
                return false;
            }
            string status = RegisterUserStatus.SUCCESS;
            if (!Regex.Match(__instance.Email, PatchConfig["UserNameRegex"]).Success)
                status = RegisterUserStatus.ERROR_USERNAME_INVALID_FORMAT;
            __instance._cachedEmailValidations[__instance.Email] = status;
            __result = status;
            return false;
        }

        [HarmonyPostfix]
        [HarmonyPatch(typeof(UI_Window_AccountCreation), "CreateAccount")]
        public static void CreateAccount(ref UI_Window_AccountCreation __instance, ref string inUsername)
        {
            if (PatchConfig.GetValueOrDefault("FlexibleDisplayNames", "false").ToLower() == "true")
            {
                inUsername = inUsername.ToLower();
            }
        }
    }
}
