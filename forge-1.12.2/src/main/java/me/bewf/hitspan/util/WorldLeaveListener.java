package me.bewf.hitspan.util;

import me.bewf.hitspan.Knockback.util.KnockbackTracker;
import me.bewf.hitspan.Range.util.RangeTracker;
import me.bewf.hitspan.combo.util.ComboTracker;
import me.bewf.hitspan.cps.util.CpsTracker;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Clears every tracker when the client leaves a world (disconnect, quitting to menu,
 * switching server or dimension) so stale values don't carry over.
 */
public final class WorldLeaveListener {

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event == null || event.getWorld() == null || !event.getWorld().isRemote) return;

        if (RangeTracker.INSTANCE != null) RangeTracker.INSTANCE.reset();
        if (ComboTracker.INSTANCE != null) ComboTracker.INSTANCE.reset();
        KnockbackTracker.reset();
        CpsTracker.reset();
    }
}
