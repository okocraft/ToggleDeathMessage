package net.okocraft.deathmessageflags;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public final class NMSDeathMessageGetter {

    private static Class<?> entityPlayerClass;
    private static Class<?> combatTrackerClass;
    private static Class<?> iChatBaseComponentClass;
    private static Class<?> chatSerializerClass = null;

    private static Method getJsonDeathMessage;


    static {
        try {
            entityPlayerClass = getNmsClass("EntityPlayer");
            combatTrackerClass = getNmsClass("CombatTracker");
            iChatBaseComponentClass = getNmsClass("IChatBaseComponent");
            for (Class<?> inner : iChatBaseComponentClass.getDeclaredClasses()) {
                if (inner.getCanonicalName().endsWith("ChatSerializer")) {
                    chatSerializerClass = inner;
                }
            }
            getJsonDeathMessage = getMethod(chatSerializerClass, String.class, iChatBaseComponentClass);
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static TranslatableComponent getDeathMessage(PlayerDeathEvent event) {
        Object iChatBaseComponent = getDeathMessageObject(event.getEntity());
        if (iChatBaseComponent == null) {
            return new TranslatableComponent();
        }

        String jsonStringDeathMessage;
        try {
            jsonStringDeathMessage = (String) getJsonDeathMessage.invoke(null, iChatBaseComponent);
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
            return new TranslatableComponent();
        }

        return (TranslatableComponent) ComponentSerializer.parse(jsonStringDeathMessage)[0];
    }

    public static void sendComponentViaNMS(Player bukkitPlayer, Object iChatBaseComponent) {
        if (iChatBaseComponent == null) {
            return;
        }

        try {
            Method sendMessage = entityPlayerClass.getMethod("sendMessage", iChatBaseComponentClass, UUID.class);
            sendMessage.invoke(getEntityPlayer(bukkitPlayer), iChatBaseComponent, new UUID(0L, 0L));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * Checks if death message is changed.
     * 
     * @param event player death event to retrieve changed death message.
     * @return true if death message is changed, otherwise false.
     */
    public static boolean isDeathMessageChanged(PlayerDeathEvent event) {
        Player bukkitPlayer = event.getEntity();
        Object originalDeathMessage = getDeathMessageObject(bukkitPlayer);
        if (originalDeathMessage == null) {
            return false;
        }
        try {
            Class<?> craftChatMessage = getCraftBukkitClass("util.CraftChatMessage");
            String originalDeathMessageStr = (String) craftChatMessage
                    .getMethod("fromComponent", iChatBaseComponentClass)
                    .invoke(null, originalDeathMessage);

            return !originalDeathMessageStr.equals(event.getDeathMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Create iChatBaseComponent from String.
     * 
     * @param str string to create component.
     * @return created component or null if failed.
     */
    public static Object fromString(String str) {
        try {
            return (String) getCraftBukkitClass("util.CraftChatMessage")
                    .getMethod("fromString", String.class).invoke(null, str);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets previous death message of {@code bukkitPlayer}.
     * 
     * @param bukkitPlayer player to retrieve death message.
     * @return iChatBaseComponent Object.
     */
    public static Object getDeathMessageObject(Player bukkitPlayer) {
        Object combatTracker = getCombatTracker(bukkitPlayer);
        if (combatTracker == null) {
            return null;
        }

        return invokeMethod(combatTrackerClass, "getDeathMessage", combatTracker);
    }

    private static Object getCombatTracker(Player bukkitPlayer) {
        Object entityPlayer = getEntityPlayer(bukkitPlayer);
        if (entityPlayer == null) {
            return null;
        }

        return invokeMethod(entityPlayerClass, "getCombatTracker", entityPlayer);
    }

    private static Object getEntityPlayer(Player bukkitPlayer) {
        return invokeMethod(bukkitPlayer.getClass(), "getHandle", bukkitPlayer);
    }

    public static Class<?> getNmsClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + "." + name);
    }

    public static Class<?> getCraftBukkitClass(String name) throws ClassNotFoundException {
        return Class.forName("org.bukkit.craftbukkit." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + "." + name);
    }

    private static Method getMethod(Class<?> clazz, Class<?> returnType, Class<?> ... parameterTypes) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.getReturnType().equals(returnType)) {
                continue;
            }

            Class<?>[] targetParameterTypes = method.getParameterTypes();
            if (targetParameterTypes.length != parameterTypes.length) {
                continue;
            }

            if (targetParameterTypes.length == 0 && parameterTypes.length == 0) {
                return method;
            }

            for (int i = 0; i < targetParameterTypes.length; i++) {
                if (targetParameterTypes[i].equals(parameterTypes[i])) {
                    if (i == targetParameterTypes.length - 1) {
                        return method;
                    }
                    continue;
                }
                break;
            }
        }

        return null;
    }

    private static Object invokeMethod(Class<?> clazz, String name, Object invokeObject) {
        try {
            return clazz.getMethod(name).invoke(invokeObject);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }
}