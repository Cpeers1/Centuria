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
    public class WorldObjectManagerPatch
    {
        [HarmonyPrefix]
        [HarmonyPatch(typeof(WorldObjectManager), "OnWorldObjectInfoMessage")]
        public static bool OnWorldObjectInfoMessage(WorldObjectInfoMessage message, ref WorldObjectManager __instance)
        {
            // Fix the bug with networked objects
            WorldObject obj = null;
            if (__instance._objects._objectsById.ContainsKey(message.Id))
                obj = __instance._objects._objectsById[message.Id];
            if (obj == null && message.DefId != "852" && message.DefId != "1751")
            {
                // Find in world
                Debug.Log("Loading world object: " + message.Id + "...");
                obj = QuestManager.instance.GetWorldObject(message.Id);
            }
            if (obj == null)
            {
                // Find in scene
                foreach (WorldObject wO in GameObject.FindObjectsOfType<WorldObject>())
                {
                    if (wO.Id == message.Id)
                    {
                        Debug.Log("Loading world object: " + message.Id + " from scene...");
                        obj = wO;
                        break;
                    }
                }
            }
            if (obj == null)
            {
                // Create
                Debug.Log("Creating world object: " + message.Id + "...");
                obj = __instance.CreateObject(message);
            }

            // Add if needed
            if (!__instance._objects._objectsById.ContainsKey(message.Id))
                __instance._objects.Add(obj);

            // Load object
            obj.OnObjectInfo(message);

            // Override position and rotation
            obj.transform.position = new Vector3(message.LastMove.position.x, message.LastMove.position.y, message.LastMove.position.z);
            obj.transform.rotation = new Quaternion(message.LastMove.rotation.x, message.LastMove.rotation.y, message.LastMove.rotation.z, message.LastMove.rotation.w);
            return false;
        }
    }
}
