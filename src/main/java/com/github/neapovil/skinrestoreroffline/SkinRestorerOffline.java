package com.github.neapovil.skinrestoreroffline;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.Gson;

public class SkinRestorerOffline extends JavaPlugin implements Listener
{
    private static SkinRestorerOffline instance;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final Semaphore semaphore = new Semaphore(1);
    private final Queue<RequestObject> queue = new ConcurrentLinkedQueue<>();

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
    private void serverTickStart(ServerTickStartEvent event)
    {
        if (event.getTickNumber() % 20 != 0)
        {
            return;
        }

        if (this.queue.peek() == null)
        {
            return;
        }

        if (!this.semaphore.tryAcquire())
        {
            return;
        }

        final RequestObject requestobject = this.queue.poll();

        final Player player = this.getServer().getPlayer(requestobject.uuid);

        if (player == null)
        {
            this.semaphore.release();
            return;
        }

        this.getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try
            {
                URI uri = new URI("https://api.mojang.com/users/profiles/minecraft/" + player.getName());
                HttpRequest httprequest = HttpRequest.newBuilder(uri).build();
                HttpResponse<String> httpresponse = this.httpClient.send(httprequest, HttpResponse.BodyHandlers.ofString());

                if (httpresponse.statusCode() != 200)
                {
                    return;
                }

                final Gson gson = new Gson();
                final UuidResponse uuidresponse = gson.fromJson(httpresponse.body(), UuidResponse.class);

                uri = new URI("https://sessionserver.mojang.com/session/minecraft/profile/" + uuidresponse.id + "?unsigned=false");
                httprequest = HttpRequest.newBuilder(uri).build();
                httpresponse = this.httpClient.send(httprequest, HttpResponse.BodyHandlers.ofString());

                if (httpresponse.statusCode() != 200)
                {
                    return;
                }

                final ProfileResponse profileresponse = gson.fromJson(httpresponse.body(), ProfileResponse.class);

                final ProfileResponse.Property property = profileresponse.properties.stream()
                        .filter(i -> i.name.equalsIgnoreCase("textures"))
                        .findAny()
                        .orElse(null);

                if (property == null)
                {
                    return;
                }

                this.getServer().getScheduler().runTask(this, () -> {
                    final PlayerProfile playerprofile = player.getPlayerProfile();

                    playerprofile.setProperty(new ProfileProperty("textures", property.value, property.signature));

                    if (player.isOnline())
                    {
                        player.setPlayerProfile(playerprofile);
                    }
                });
            }
            catch (Exception e)
            {
            }
            finally
            {
                this.semaphore.release();
            }
        });
    }

    @EventHandler
    private void join(PlayerJoinEvent event)
    {
        this.queue.offer(new RequestObject(event.getPlayer().getUniqueId()));
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

    class RequestObject
    {
        public final UUID uuid;

        public RequestObject(UUID uuid)
        {
            this.uuid = uuid;
        }
    }
}
