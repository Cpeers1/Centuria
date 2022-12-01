using HarmonyLib;
using Il2CppSystem.Collections.Generic;

namespace feraltweaks.Patches.AssemblyCSharp
{
    public class BaseDefPatch
    {
        [HarmonyPostfix]
        [HarmonyPatch(MethodType.Getter)]
        [HarmonyPatch(typeof(BaseDef), nameof(BaseDef.DefIDToChart))]
        public static void DefIDToChart(ref object __result)
        {
            Dictionary<string, ChartDataObject> res = (Dictionary<string, ChartDataObject>)__result;
            foreach (string id in CoreChartDataManagerPatch.DefCache.Keys)
            {
                if (res.ContainsKey(id))
                {
                    res[id].GetDef(id)._components = CoreChartDataManagerPatch.DefCache[id]._components;
                }
            }
        }

        [HarmonyPrefix]
        [HarmonyPatch(typeof(BaseDef), "GetDef")]
        public static bool GetDef(string inDefID, ref BaseDef __result)
        {
            if (CoreChartDataManagerPatch.DefCache.ContainsKey(inDefID))
            {
                __result = CoreChartDataManagerPatch.DefCache[inDefID];
                return false;
            }

            return true;
        }
    }
}
