package com.wildfire.main;

import com.wildfire.main.networking.WildfireNetwork;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod(modid = WildfireGender.MODID, name = WildfireGender.NAME, version = Tags.VERSION, acceptableRemoteVersions = "*")
public class WildfireGender {

    public static final String MODID = "femalegender";
    public static final String NAME = "Female Gender Mod";

    public static final Logger LOGGER = LogManager.getLogger(NAME);

    /**
     * Settings for every player we know about, keyed by UUID. Written from the network thread,
     * read from the render thread, hence the concurrent map.
     */
    public static final Map<UUID, GenderPlayer> CLOTHING_PLAYERS = new ConcurrentHashMap<UUID, GenderPlayer>();

    public static File configDir;

    /** UUID of the player at the keyboard, or null on a dedicated server. */
    public static UUID localPlayerUUID;

    @SidedProxy(clientSide = "com.wildfire.main.ClientProxy", serverSide = "com.wildfire.main.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configDir = event.getModConfigurationDirectory();
        WildfireNetwork.init();
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }

    public static GenderPlayer getPlayerById(UUID id) {
        return id == null ? null : CLOTHING_PLAYERS.get(id);
    }

    public static GenderPlayer getOrAddPlayerById(UUID id) {
        GenderPlayer plr = CLOTHING_PLAYERS.get(id);
        if (plr == null) {
            plr = new GenderPlayer(id);
            GenderPlayer existing = CLOTHING_PLAYERS.putIfAbsent(id, plr);
            if (existing != null) {
                return existing;
            }
        }
        return plr;
    }

    /**
     * Reads a player's settings off disk on a worker thread, so a slow filesystem cannot stall the game.
     */
    public static void loadGenderInfoAsync(final UUID uuid, final boolean markForSync) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    GenderPlayer.loadCachedPlayer(uuid, markForSync);
                } catch (Exception e) {
                    LOGGER.warn("Failed to load gender settings for " + uuid, e);
                }
            }
        });
        thread.setName("WFGM_GetPlayer-" + uuid);
        thread.setDaemon(true);
        thread.start();
    }
}
