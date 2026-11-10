package com.main.fast_irons_spellbooks_addition.network;

import com.main.fast_irons_spellbooks_addition.spells.physical.HeroResonanceSpell;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端：同步某个实体当前的勇者调律元素。
 * 客户端持久化数据拿不到服务端的值，此前只能回退到静态索引表，
 * 而该表在多人游戏下不会被客户端填充，导致元素显示恒为默认火焰。
 */
public class SyncHeroElementPacket {
    private final UUID entityId;
    private final int elementOrdinal;

    public SyncHeroElementPacket(UUID entityId, int elementOrdinal) {
        this.entityId = entityId;
        this.elementOrdinal = elementOrdinal;
    }

    public SyncHeroElementPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readUUID();
        this.elementOrdinal = buf.readByte();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(entityId);
        buf.writeByte(elementOrdinal);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> HeroResonanceSpell.elementIndexMap.put(entityId, elementOrdinal));
        return true;
    }
}