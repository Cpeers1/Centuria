using BepInEx;
using BepInEx.Logging;
using HarmonyLib;
using Il2CppSystem;
using Il2CppSystem.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Threading;

namespace feraltweaks.Patches.AssemblyCSharp
{
    public class CoreChartDataManagerPatch
    {
        public static Dictionary<string, BaseDef> DefCache = new Dictionary<string, BaseDef>();

        [HarmonyPostfix]
        [HarmonyPatch(typeof(CoreChartDataManager), "SetChartObjectInstances")]
        static void SetChartObjectInstances()
        {
            // Okay time to load over the original game
            ManualLogSource logger = Plugin.logger;

            // Create patch directory
            logger.LogInfo("Loading chart patches...");
            Directory.CreateDirectory(Paths.ConfigPath + "/feraltweaks/chartpatches");

            // Read patches
            foreach (FileInfo file in new DirectoryInfo(Paths.ConfigPath + "/feraltweaks/chartpatches").GetFiles("*.cdpf", SearchOption.AllDirectories))
            {
                logger.LogInfo("Loading patch: " + file.Name);
                string patch = File.ReadAllText(file.FullName).Replace("\t", "    ").Replace("\r", "");

                // Parse patch
                bool inPatchBlock = false;
                string defID = "";
                string patchData = "";
                ChartDataObject chart = null;
                foreach (string line in patch.Split('\n'))
                {
                    if (!inPatchBlock)
                    {
                        if (line == "" || line.StartsWith("//") || line.StartsWith("#"))
                            continue;

                        // Check command
                        System.Collections.Generic.List<string> args = new System.Collections.Generic.List<string>(line.Split(" "));
                        if (args.Count <= 1)
                        {
                            logger.LogError("Invalid command: " + line + " found while parsing " + file.Name);
                            break;
                        }
                        else
                        {
                            string cmd = args[0];
                            args.RemoveAt(0);

                            bool error = false;
                            switch (cmd)
                            {
                                case "setchart":
                                    {
                                        string chartName = args[0];
                                        switch (chartName)
                                        {
                                            case "WorldObjectChart":
                                                {
                                                    chart = ChartDataManager.instance.worldObjectChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.worldObjectChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "LocalizationChart":
                                                {
                                                    chart = ChartDataManager.instance.localizationChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.localizationChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "ActorAttachNodeChart":
                                                {
                                                    chart = ChartDataManager.instance.actorAttachNodeChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.actorAttachNodeChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "CalendarChart":
                                                {
                                                    chart = ChartDataManager.instance.calendarChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.calendarChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "DialogChart":
                                                {
                                                    chart = ChartDataManager.instance.dialogChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.dialogChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "ActorScaleGroupChart":
                                                {
                                                    chart = ChartDataManager.instance.actorScaleGroupChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.actorScaleGroupChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "ActorAttachNodeGroupChart":
                                                {
                                                    chart = ChartDataManager.instance.actorAttachNodeGroupChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.actorAttachNodeGroupChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "ActorNPCChart":
                                                {
                                                    chart = ChartDataManager.instance.actorNPCChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.actorNPCChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "ActorBodyPartNodeChart":
                                                {
                                                    chart = ChartDataManager.instance.actorBodyPartNodeChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.actorBodyPartNodeChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "WorldSurfaceChart":
                                                {
                                                    chart = ChartDataManager.instance.worldSurfaceChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.worldSurfaceChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "URLChart":
                                                {
                                                    chart = ChartDataManager.instance.urlChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.urlChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "CraftableItemChart":
                                                {
                                                    chart = ChartDataManager.instance.craftableItemChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.craftableItemChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "AudioChart":
                                                {
                                                    chart = ChartDataManager.instance.audioChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.audioChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "HarvestPointChart":
                                                {
                                                    chart = ChartDataManager.instance.harvestPointChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.harvestPointChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "QuestChart":
                                                {
                                                    chart = ChartDataManager.instance.questChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.questChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "ObjectiveChart":
                                                {
                                                    chart = ChartDataManager.instance.objectiveChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.objectiveChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "TaskChart":
                                                {
                                                    chart = ChartDataManager.instance.taskChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.taskChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "InteractableChart":
                                                {
                                                    chart = ChartDataManager.instance.interactableChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.interactableChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "QuestNPCDataChart":
                                                {
                                                    chart = ChartDataManager.instance.questNpcDataChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.questNpcDataChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "BundleIDChart":
                                                {
                                                    chart = ChartDataManager.instance.bundleIDChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.bundleIDChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "BundlePackChart":
                                                {
                                                    chart = ChartDataManager.instance.bundlePackChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.bundlePackChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "ShopContentChart":
                                                {
                                                    chart = ChartDataManager.instance.shopContentChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.shopContentChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "ShopChart":
                                                {
                                                    chart = ChartDataManager.instance.shopChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.shopChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "LootChart":
                                                {
                                                    chart = ChartDataManager.instance.lootChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.lootChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "NetworkedObjectsChart":
                                                {
                                                    chart = ChartDataManager.instance.networkedObjectsChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.networkedObjectsChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "ColorChart":
                                                {
                                                    chart = ChartDataManager.instance.colorChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.colorChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            case "GlobalChart":
                                                {
                                                    chart = ChartDataManager.instance.globalChartData;

                                                    // Let it load
                                                    while (chart == null)
                                                    {
                                                        chart = ChartDataManager.instance.globalChartData;
                                                        Thread.Sleep(100);
                                                    }
                                                    break;
                                                }
                                            default:
                                                {
                                                    logger.LogError("Invalid command: " + line + " found while parsing " + file.Name + ": chart not recognized");
                                                    error = true;
                                                    break;
                                                }
                                        }
                                        break;
                                    }
                                case "cleardef":
                                    {
                                        if (chart == null)
                                        {
                                            logger.LogError("Invalid command: " + line + " found while parsing " + file.Name + ": no active chart set");
                                            error = true;
                                            break;
                                        }
                                        logger.LogInfo("Clear def: " + args[0]);
                                        BaseDef def = chart.GetDef(args[0]);
                                        if (def == null)
                                            logger.LogError("Error! Definition not found!");
                                        else
                                        {
                                            def._components._components.Clear();
                                            DefCache[args[0]] = def;
                                        }
                                        break;
                                    }
                                case "patch":
                                    {
                                        if (chart == null)
                                        {
                                            logger.LogError("Invalid command: " + line + " found while parsing " + file.Name + ": no active chart set");
                                            error = true;
                                            break;
                                        }
                                        inPatchBlock = true;
                                        patchData = "";
                                        defID = args[0];
                                        break;
                                    }
                                default:
                                    {
                                        logger.LogError("Invalid command: " + line + " found while parsing " + file.Name);
                                        error = true;
                                        break;
                                    }
                            }
                            if (error)
                                break;
                        }
                    }
                    else
                    {
                        string l = line;
                        if (l == "endpatch")
                        {
                            // Apply patch
                            inPatchBlock = false;
                            string chartPatch = patchData;
                            patchData = "";

                            // Get def
                            logger.LogInfo("Patching " + defID + " in chart " + chart.ChartName);
                            BaseDef def = chart.GetDef(defID, true);
                            if (def == null)
                                logger.LogError("Error! Definition not found!");
                            else
                            {
                                PatchDef(chartPatch, def);
                                DefCache[defID] = def;
                            }
                            continue;
                        }
                        for (int i = 0; i < 4; i++)
                        {
                            if (!l.StartsWith(" "))
                                break;
                            l = l.Substring(1);
                        }
                        if (patchData == "")
                            patchData = l;
                        else
                            patchData += l + "\n";
                    }
                }
            }
        }

        private static void PatchDef(string chartPatch, BaseDef def)
        {
            if (def == null)
                return;
            // Patch
            Dictionary<Type, List<ComponentBase>> components = new Dictionary<Type, List<ComponentBase>>();
            if (def._components != null && def._components._components != null)
            {
                foreach (Type t in def._components._components.Keys)
                {
                    components[t] = def._components._components[t];
                }
                def._components._components.Clear();
            }
            def.LoadDataJSON(chartPatch);

            if (def._components != null && def._components._components != null)
            {
                foreach (Type t in components.Keys)
                {
                    List<ComponentBase> lst = new List<ComponentBase>();
                    if (def._components._components.ContainsKey(t))
                        lst = def._components._components[t];
                    else
                        def._components._components[t] = lst;
                    foreach (ComponentBase comp in components[t])
                    {
                        lst.Add(comp);
                    }
                }
            }
        }
    }
}
