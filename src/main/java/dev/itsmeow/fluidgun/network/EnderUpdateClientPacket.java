package dev.itsmeow.fluidgun.network;

import dev.itsmeow.fluidgun.client.ClientEvents;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class EnderUpdateClientPacket {

    public boolean notify;
    public int slot;
    public int count;
    public int max;
    public TankNetwork[] tanks;

    public static class TankNetwork {

        public int contained;
        public int max;
        public String fluidTranslationKey;
        public int temp;

        public TankNetwork(int contained, int max, String fluidTranslationKey, int temp) {
            this.contained = contained;
            this.max = max;
            this.fluidTranslationKey = fluidTranslationKey;
            this.temp = temp;
        }

        public void encode(PacketBuffer buf) {
            buf.writeInt(contained);
            buf.writeInt(max);
            if(contained > 0) {
                buf.writeInt(temp);
                buf.writeString(fluidTranslationKey, 64);
            }
        }

        public static TankNetwork decode(PacketBuffer buf) {
            int contained = buf.readInt();
            int max = buf.readInt();
            int temp = 0;
            String fluidTranslationKey = "";
            if(contained > 0) {
                temp = buf.readInt();
                fluidTranslationKey = buf.readString(64);
            }
            return new TankNetwork(contained, max, fluidTranslationKey, temp);
        }

        public static TankNetwork stack(FluidStack stack, int max) {
            return new TankNetwork(stack.getAmount(), max, stack.getTranslationKey(), stack.getFluid().getAttributes().getTemperature());
        }

        public static TankNetwork[] fromHandler(IFluidHandler handler) {
            TankNetwork[] arr = new TankNetwork[handler.getTanks()];
            for(int i = 0; i < handler.getTanks(); i++) {
                arr[i] = TankNetwork.stack(handler.getFluidInTank(i), handler.getTankCapacity(i));
            }
            return arr;
        }
    }

    public EnderUpdateClientPacket(boolean notify, int slot, int count, int max, IFluidHandler handler) {
        this(notify, slot, count, max, TankNetwork.fromHandler(handler));
    }

    public EnderUpdateClientPacket(boolean notify, int slot, int count, int max, TankNetwork[] tanks) {
        this.notify = notify;
        this.slot = slot;
        this.count = count;
        this.max = max;
        this.tanks = tanks;
    }

    public static void encode(EnderUpdateClientPacket pkt, PacketBuffer buf) {
        buf.writeBoolean(pkt.notify);
        buf.writeInt(pkt.slot);
        buf.writeInt(pkt.count);
        buf.writeInt(pkt.max);
        buf.writeInt(pkt.tanks.length);
        for(TankNetwork tank : pkt.tanks) {
            tank.encode(buf);
        }
    }

    public static EnderUpdateClientPacket decode(PacketBuffer buf) {
        boolean notify = buf.readBoolean();
        int slot = buf.readInt();
        int count = buf.readInt();
        int max = buf.readInt();
        TankNetwork[] tanks = new TankNetwork[buf.readInt()];
        for(int i = 0; i < tanks.length; i++) {
            tanks[i] = TankNetwork.decode(buf);
        }
        return new EnderUpdateClientPacket(notify, slot, count, max, tanks);
    }

    public static class Handler {

        public static void handle(EnderUpdateClientPacket msg, Supplier<NetworkEvent.Context> ctx) {
            if(ctx.get().getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
                ctx.get().setPacketHandled(false);
                return;
            }
            ctx.get().enqueueWork(() -> {
                ClientEvents.onEnderFired(msg);
            });
            ctx.get().setPacketHandled(true);
        }

    }

}
