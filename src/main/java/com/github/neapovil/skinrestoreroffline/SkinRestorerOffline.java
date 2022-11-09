package com.github.neapovil.skinrestoreroffline;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.Gson;

public class SkinRestorerOffline extends JavaPlugin implements Listener
{
    private static SkinRestorerOffline instance;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public void onEnable()
    {
        if (this.getServer().getOnlineMode())
        {
            this.getLogger().severe("The server has Online Mode enabled. I have nothing to restore!");

            this.getPluginLoader().disablePlugin(this);

            return;
        }

        instance = this;

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable()
    {
    }

    public static SkinRestorerOffline getInstance()
    {
        return instance;
    }

    @EventHandler
    private void join(AsyncPlayerPreLoginEvent event)
    {
        URI uri = null;

        try
        {
            uri = new URI("https://api.mojang.com/users/profiles/minecraft/" + event.getName());
        }
        catch (URISyntaxException e)
        {
        }

        if (uri == null)
        {
            return;
        }

        final HttpRequest httprequest = HttpRequest.newBuilder(uri).build();

        HttpResponse<String> httpresponse = null;

        try
        {
            httpresponse = this.httpClient.send(httprequest, BodyHandlers.ofString());
        }
        catch (Exception e)
        {
        }

        if (httpresponse == null)
        {
            return;
        }

        if (httpresponse.statusCode() != 200)
        {
            return;
        }

        final Gson gson = new Gson();

        final UuidResponse uuidresponse = gson.fromJson(httpresponse.body(), UuidResponse.class);

        URI uri1 = null;

        try
        {
            uri1 = new URI("https://sessionserver.mojang.com/session/minecraft/profile/" + uuidresponse.id + "?unsigned=false");
        }
        catch (URISyntaxException e)
        {
        }

        if (uri1 == null)
        {
            return;
        }

        final HttpRequest httprequest1 = HttpRequest.newBuilder(uri1).build();

        HttpResponse<String> httpresponse1 = null;

        try
        {
            httpresponse1 = this.httpClient.send(httprequest1, BodyHandlers.ofString());
        }
        catch (Exception e)
        {
        }

        if (httpresponse1 == null)
        {
            return;
        }

        if (httpresponse1.statusCode() != 200)
        {
            return;
        }

        final ProfileResponse profileresponse = gson.fromJson(httpresponse1.body(), ProfileResponse.class);

        if (profileresponse.properties.isEmpty())
        {
            return;
        }

        if (!profileresponse.properties.get(0).name.equalsIgnoreCase("textures"))
        {
            return;
        }

        final PlayerProfile playerprofile = event.getPlayerProfile();

        playerprofile.setProperty(new ProfileProperty("textures", profileresponse.properties.get(0).value, profileresponse.properties.get(0).signature));
    }

    class UuidResponse
    {
        public String id;
    }

    class ProfileResponse
    {
        public List<Property> properties;

        class Property
        {
            public String name;
            public String value;
            public String signature;
        }
    }
}
