package com.github.neapovil.skinrestoreroffline;

import org.bukkit.plugin.java.JavaPlugin;

public class SkinRestorerOffline extends JavaPlugin
{
    private static SkinRestorerOffline instance;

    @Override
    public void onEnable()
    {
        instance = this;
    }

    @Override
    public void onDisable()
    {
    }

    public static SkinRestorerOffline getInstance()
    {
        return instance;
    }
}
