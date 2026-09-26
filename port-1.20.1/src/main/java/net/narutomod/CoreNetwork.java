package net.narutomod;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/** Server-authoritative synchronization for the small core player snapshot. */
public final class CoreNetwork {
    private static final String PROTOCOL = "1";
    private static int packetId;
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(NarutoMod.MODID, "core"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private CoreNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(packetId++, SyncPacket.class, SyncPacket::encode, SyncPacket::decode, SyncPacket::handle);
    }

    public static void send(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPacket(CoreData.snapshot(player)));
    }

    public static final class SyncPacket {
        private final CompoundTag data;

        public SyncPacket(CompoundTag data) {
            this.data = data;
        }

        private static void encode(SyncPacket packet, FriendlyByteBuf buffer) {
            buffer.writeNbt(packet.data);
        }

        private static SyncPacket decode(FriendlyByteBuf buffer) {
            CompoundTag data = buffer.readNbt();
            return new SyncPacket(data == null ? new CompoundTag() : data);
        }

        private static void handle(SyncPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        if (Minecraft.getInstance().player != null) {
                            CoreData.applySnapshot(Minecraft.getInstance().player, packet.data);
                        }
                    }));
            context.setPacketHandled(true);
        }
    }
}
