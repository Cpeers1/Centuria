using HarmonyLib;
using System.IO;
using UnityEngine;

namespace feraltweaks.Patches.AssemblyCSharp
{
    public class CoreBundleManager2Patch
    {
        [HarmonyPrefix]
        [HarmonyPatch(typeof(CoreBundleManager2), "InternalDownloadBundleRoutine")]
        public static bool InternalDownloadBundleRoutine(ManifestDef inDef, CoreBundleManager2.LoadedAssetBundleEntry inLoadedAssetBundleEntry, ref CoreBundleManager2 __instance)
        {
            if (File.Exists(inDef.BundleCacheFilePath))
            {
                Debug.Log("Cancelled bundle download of " + inDef.defID + ": already exists");
                return false; // Already exists
            }
            Debug.Log("Downloading bundle: " + inDef.defID);
            return true;
        }
    }
}
