package app.joshie.shatteredskies.tick;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.asset.type.blocktick.config.TickProcedure;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import javax.annotation.Nonnull;


public final class TeslaShockTickProcedure extends TickProcedure {
    public static final BuilderCodec<TeslaShockTickProcedure> CODEC = BuilderCodec.builder(
                    TeslaShockTickProcedure.class,
                    TeslaShockTickProcedure::new,
                    TickProcedure.BASE_CODEC
            )
            .addField(new KeyedCodec<>("Radius", Codec.FLOAT), (p, v) -> p.radius = v, p -> p.radius)
            .addField(new KeyedCodec<>("ChanceMin", Codec.INTEGER), (p, v) -> p.chanceMin = v, p -> p.chanceMin)
            .addField(new KeyedCodec<>("Chance", Codec.INTEGER), (p, v) -> p.chance = v, p -> p.chance)
            .addField(new KeyedCodec<>("IntervalTicks", Codec.INTEGER), (p, v) -> {
                p.chanceMin = 1;
                p.chance = v;
            }, p -> p.chance)
            .addField(new KeyedCodec<>("Damage", Codec.FLOAT), (p, v) -> p.damage = v, p -> p.damage)
            .addField(new KeyedCodec<>("MaxTargets", Codec.INTEGER), (p, v) -> p.maxTargets = v, p -> p.maxTargets)
            .addField(new KeyedCodec<>("ParticleSystemId", Codec.STRING), (p, v) -> p.particleSystemId = v, p -> p.particleSystemId)
            .addField(new KeyedCodec<>("BeamParticleSystemId", Codec.STRING), (p, v) -> p.beamParticleSystemId = v, p -> p.beamParticleSystemId)
            .addField(new KeyedCodec<>("BeamBaseLength", Codec.FLOAT), (p, v) -> p.beamBaseLength = v, p -> p.beamBaseLength)
            .addField(new KeyedCodec<>("ShootParticleSystemId", Codec.STRING), (p, v) -> p.shootParticleSystemId = v, p -> p.shootParticleSystemId)
            .addField(new KeyedCodec<>("ShootSoundEventId", Codec.STRING), (p, v) -> p.shootSoundEventId = v, p -> p.shootSoundEventId)
            .addField(new KeyedCodec<>("ImpactSoundEventId", Codec.STRING), (p, v) -> p.impactSoundEventId = v, p -> p.impactSoundEventId)
            .addField(new KeyedCodec<>("SoundVolume", Codec.FLOAT), (p, v) -> p.soundVolume = v, p -> p.soundVolume)
            .addField(new KeyedCodec<>("SoundPitch", Codec.FLOAT), (p, v) -> p.soundPitch = v, p -> p.soundPitch)
            .build();

    private float radius = 6.0f;
    private int chanceMin = 1;
    private int chance = 20;
    private float damage = 2.0f;
    private int maxTargets = 3;
    private String particleSystemId = "TeslaShock_Lightning";
    private String beamParticleSystemId = "TeslaShock_Beam";
    private float beamBaseLength = 20.0f;
    private String shootParticleSystemId = "TeslaShock_Shoot_Yellow";
    private String shootSoundEventId = "";
    private String impactSoundEventId = "";
    private float soundVolume = 1.0f;
    private float soundPitch = 1.0f;

    public TeslaShockTickProcedure() {
    }

    @Nonnull
    @Override
    public BlockTickStrategy onTick(@Nonnull World world, @Nonnull WorldChunk chunk, int worldX, int worldY, int worldZ, int blockId) {
        if (!this.runChance()) {
            return BlockTickStrategy.CONTINUE;
        }

        Store<EntityStore> store = world.getEntityStore().getStore();
        SpatialResource<Ref<EntityStore>, EntityStore> spatial = store.getResource(EntityModule.get().getEntitySpatialResourceType());
        if (spatial == null) {
            return BlockTickStrategy.CONTINUE;
        }



        Vector3d origin = new Vector3d(worldX + 0.5, worldY + 0.75, worldZ + 0.5);
        ObjectList<Ref<EntityStore>> refs = SpatialResource.getThreadLocalReferenceList();
        spatial.getSpatialStructure().collect(origin, this.radius, refs);

        if (refs.isEmpty()) {
            return BlockTickStrategy.CONTINUE;
        }

        int envCauseIndex = DamageCause.getAssetMap().getIndex("Environment");
        if (envCauseIndex == Integer.MIN_VALUE) {
            envCauseIndex = DamageCause.getAssetMap().getIndex("Command");
        }
        if (envCauseIndex == Integer.MIN_VALUE) {
            envCauseIndex = 0;
        }

        int shocked = 0;
        for (int i = 0; i < refs.size() && shocked < this.maxTargets; i++) {
            Ref<EntityStore> ref = refs.get(i);
            if (!ref.isValid()) continue;
            if (store.getComponent(ref, Player.getComponentType()) != null) {
                return BlockTickStrategy.CONTINUE;
            }

            EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
            if (stats == null) continue;

            TransformComponent tx = store.getComponent(ref, TransformComponent.getComponentType());
            if (tx == null) continue;

            Vector3d pos = tx.getPosition();
            double dx = pos.x - origin.x;
            double dy = pos.y - origin.y;
            double dz = pos.z - origin.z;
            double dist2 = dx * dx + dy * dy + dz * dz;
            if (dist2 > (double) this.radius * (double) this.radius) continue;

            Damage damageEvent = new Damage(new Damage.EnvironmentSource("TeslaShock"), envCauseIndex, this.damage);
            DamageSystems.executeDamage(ref, store, damageEvent);

            Vector3d target = new Vector3d(pos.x, pos.y + 1.0, pos.z);
            Vector3d dir = new Vector3d(target.x - origin.x, target.y - origin.y, target.z - origin.z);
            double len = Math.sqrt(dir.x * dir.x + dir.y * dir.y + dir.z * dir.z);
            if (len > 0.001) {
                float heading = PhysicsMath.headingFromDirection(dir.x, dir.z);
                float pitch = PhysicsMath.pitchFromDirection(dir.x, dir.y, dir.z);
                float scale = (float) (len / Math.max(0.1f, this.beamBaseLength));
                SpatialResource<Ref<EntityStore>, EntityStore> playerSpatial = store.getResource(EntityModule.get().getPlayerSpatialResourceType());
                if (playerSpatial != null) {
                    ObjectList<Ref<EntityStore>> playerRefs = SpatialResource.getThreadLocalReferenceList();
                    playerRefs.clear();
                    Vector3d mid = new Vector3d((origin.x + target.x) * 0.5, (origin.y + target.y) * 0.5, (origin.z + target.z) * 0.5);
                    playerSpatial.getSpatialStructure().collect(mid, 110.0, playerRefs);
                    if (!playerRefs.isEmpty()) {
                        ParticleUtil.spawnParticleEffect(
                                this.beamParticleSystemId,
                                mid.x, mid.y, mid.z,
                                heading, pitch, 0.0f,
                                scale,
                                null,
                                null,
                                playerRefs,
                                store
                        );
                    }
                }
            }

            ParticleUtil.spawnParticleEffect(this.particleSystemId, target, store);

            playSoundIfConfigured(this.impactSoundEventId, target.x, target.y, target.z, store);
            shocked++;
        }

        if (shocked > 0) {
            ParticleUtil.spawnParticleEffect(this.shootParticleSystemId, origin, store);
            playSoundIfConfigured(this.shootSoundEventId, origin.x, origin.y, origin.z, store);
        }

        return BlockTickStrategy.CONTINUE;
    }

    private boolean runChance() {
        int chance = Math.max(1, this.chance);
        int chanceMin = Math.max(0, Math.min(this.chanceMin, chance));
        return this.getRandom().nextInt(chance) < chanceMin;
    }

    private void playSoundIfConfigured(@Nonnull String soundEventId, double x, double y, double z, @Nonnull Store<EntityStore> store) {
        if (soundEventId.isBlank()) return;
        int idx = SoundEvent.getAssetMap().getIndex(soundEventId);
        if (idx == Integer.MIN_VALUE || idx == 0) return;
        SoundUtil.playSoundEvent3d(idx, SoundCategory.SFX, x, y, z, this.soundVolume, this.soundPitch, store);
    }
}







