package app.joshie.shatteredskies;


import app.joshie.shatteredskies.tick.TeslaShockTickProcedure;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktick.config.TickProcedure;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class ShatteredSkies extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public ShatteredSkies(JavaPluginInit init) {
        super(init);
        TickProcedure.CODEC.register("TeslaShock", TeslaShockTickProcedure.class, TeslaShockTickProcedure.CODEC);
    }
}
