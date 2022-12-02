using HarmonyLib;
using System;
using System.Collections;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using UnityEngine;
using WW.Waiters;

namespace feraltweaks.Patches.AssemblyCSharp
{
    public class LoginLogoutPatches
    {
        private static bool loggingOut = false;
        private static List<Action> actionsToRun = new List<Action>();
        private static LoadingScreenAction loadWaiter;
        private class LoadingScreenAction
        {
            public Action action;
            public bool inited = false;
            public float stamp = 0;
        }

        [HarmonyPostfix]
        [HarmonyPatch(typeof(WaitController), "Update")]
        private static void Update(ref WaitController __instance)
        {
            LoadingScreenAction waiter = loadWaiter;
            if (waiter != null && UI_ProgressScreen.instance.IsVisible && !UI_ProgressScreen.instance.IsFading && DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() >= waiter.stamp)
            {
                if (!waiter.inited)
                {
                    waiter.stamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() + 3000;
                    waiter.inited = true;
                    return;
                }
                loadWaiter = null;
                waiter.action.Invoke();
            }

            if (actionsToRun.Count != 0)
            {
                Action[] actions;
                while (true)
                {
                    try
                    {
                        actions = actionsToRun.ToArray();
                        break;
                    }
                    catch { }
                }
                foreach (Action ac in actions)
                {
                    actionsToRun.Remove(ac);
                    ac.Invoke();
                }
            }
        }

        [HarmonyPrefix]
        [HarmonyPatch(typeof(CoreSharedUtils), "CoreReset", new Type[] { typeof(SplashError), typeof(ErrorCode) })]
        public static bool CoreReset()
        {
            if (loggingOut)
                return false;
            loggingOut = true;
            if (CoreManagerBase<CoreNotificationManager>.coreInstance != null)
            {
                CoreManagerBase<CoreNotificationManager>.coreInstance.ClearAndScheduleAllLocalNotifications();
            }
            CoreBundleManager2.UnloadAllLevelAssetBundles();
            UI_ProgressScreen.instance.ClearLabels();
            UI_ProgressScreen.instance.SetSpinnerLabelWithIndex(0, "Logging Out...");
            RoomManager.instance.PreviousLevelDef = ChartDataManager.instance.levelChartData.GetLevelDefWithUnityLevelName("Main_Menu");
            RoomManager.instance.CurrentLevelDef = ChartDataManager.instance.levelChartData.GetLevelDefWithUnityLevelName("CityFera");
            UI_ProgressScreen.instance.UpdateLevel();
            CoreLoadingManager.ShowProgressScreen(null);
            loadWaiter = new LoadingScreenAction()
            {
                action = () =>
                {
                    RoomManager.instance.CurrentLevelDef = ChartDataManager.instance.levelChartData.GetLevelDefWithUnityLevelName("Main_Menu");
                    if (NetworkManager.instance._serverConnection.IsConnected)
                    {
                        NetworkManager.instance._serverConnection.Disconnect();
                        if (NetworkManager.instance._chatServiceConnection.IsConnected)
                            NetworkManager.instance._chatServiceConnection.Disconnect();
                    }
                    RoomManager.instance.StartCoroutine(LoadingManager.instance.LoadLevel(RoomManager.instance.CurrentLevelDef.UnityLevelName, RoomManager.instance.CurrentLevelDef.AdditiveUnityLevelNames));
                    RoomManager.instance.IsLevelFinishedLoading = true;
                    CoreWindowManager.CloseAllWindows();
                    CoreLoadingManager.HideProgressScreen();
                    CoreWindowManager.OpenWindow<UI_Window_Login>(null, true);
                    CoreBundleManager2.UnloadAllLevelAssetBundles();
                    loggingOut = false;
                }
            };
            return false;
        }

        [HarmonyPrefix]
        [HarmonyPatch(typeof(UI_Window_Login), "OnOpen")]
        public static void OnOpen(UI_Window_Login __instance)
        {
            RoomManager.instance.PreviousLevelDef = ChartDataManager.instance.levelChartData.GetLevelDefWithUnityLevelName("Main_Menu");
        }

        [HarmonyPrefix]
        [HarmonyPatch(typeof(UI_Window_Login), "BtnClicked_Login")]
        public static void BtnClicked_Login(UI_Window_Login __instance)
        {
            UI_ProgressScreen.instance.ClearLabels();
            UI_ProgressScreen.instance.SetSpinnerLabelWithIndex(0, "Logging In...");
            RoomManager.instance.PreviousLevelDef = ChartDataManager.instance.levelChartData.GetLevelDefWithUnityLevelName("Main_Menu");
            UI_ProgressScreen.instance.UpdateLevel();
            CoreLoadingManager.ShowProgressScreen(null);
        }
    }
}
