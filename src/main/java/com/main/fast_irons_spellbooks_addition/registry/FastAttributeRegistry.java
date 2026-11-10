package com.main.fast_irons_spellbooks_addition.registry;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import io.redspace.ironsspellbooks.api.attribute.MagicPercentAttribute;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(
        modid = FastIronsSpellbooksAddition.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public class FastAttributeRegistry {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, FastIronsSpellbooksAddition.MODID);

    public static void register(IEventBus bus) {
        ATTRIBUTES.register(bus);
    }

    public static final RegistryObject<Attribute> PHYSICAL_POWER =
            ATTRIBUTES.register("physical_power", () ->
                    new MagicPercentAttribute(
                            "attribute.fast_irons_spellbooks_addition.physical_power",
                            1.0D,
                            -100.0D,
                            100.0D
                    ).setSyncable(true)
            );

    public static final RegistryObject<Attribute> PHYSICAL_RESIST =
            ATTRIBUTES.register("physical_resist", () ->
                    new MagicPercentAttribute(
                            "attribute.fast_irons_spellbooks_addition.physical_resist",
                            1.0D,
                            -100.0D,
                            100.0D
                    ).setSyncable(true)
            );

    public static final RegistryObject<Attribute> MAGIC_ATTACK =
            ATTRIBUTES.register("magic_attack", () ->
                    new RangedAttribute(
                            "attribute.fast_irons_spellbooks_addition.magic_attack",
                            1.0D,
                            0.0D,
                            23333.0D
                    )
            );
            
    public static final RegistryObject<Attribute> BASIC_PHYSICAL_DAMAGE =
            ATTRIBUTES.register("basic_physical_damage", () ->
                    new RangedAttribute(
                            "attribute.fast_irons_spellbooks_addition.basic_physical_damage",
                            1.0D,
                            -100.0D,
                            100.0D
                    )
            );
            
    public static final RegistryObject<Attribute> BASIC_MAGIC_DAMAGE =
            ATTRIBUTES.register("basic_magic_damage", () ->
                    new RangedAttribute(
                            "attribute.fast_irons_spellbooks_addition.basic_magic_damage",
                            1.0D,
                            -100.0D,
                            100.0D
                    )
            );

    /**
     * 玩家可同时存在的「自动施法随从」数量上限。
     *
     * <p>基础值 1、取值范围 0~32：默认只能带一只，以后每加一件联动装备
     * （饰品 / 武器 / 套装）就给玩家 +1，最多叠到 32。
     * 召唤时由 {@code PhantomCantor#summon} 检查，超了就解雇最旧的那只，
     * 所以所有联动共用一个上限，不需要各自维护占位逻辑。</p>
     *
     * <p><b>注意区分「默认值」和「上限」</b>：构造函数里第一个 1.0D 是
     * {@code defaultValue}（没人写它时读多少），最后一个 32.0D 才是
     * {@code maxValue}（真正能拦住数值的那道闸）。{@code AttributeInstance#getValue()}
     * 每次算完都会走 {@code sanitizeValue} 把结果 clamp 进 {@code [min, max]}。
     * 所以只要有人 {@code setBaseValue(2)}，读出来就是 2 —— 这是属性的设计，不是上限失效。</p>
     */
    public static final RegistryObject<Attribute> CANTOR_LIMIT =
            ATTRIBUTES.register("cantor_limit", () ->
                    new RangedAttribute(
                            "attribute.fast_irons_spellbooks_addition.cantor_limit",
                            1.0D,
                            0.0D,
                            32.0D
                    ).setSyncable(true)
            );

    @SubscribeEvent
    public static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(entity -> {
            event.add(entity, PHYSICAL_POWER.get());
            event.add(entity, PHYSICAL_RESIST.get());
            event.add(entity, MAGIC_ATTACK.get());
            event.add(entity, BASIC_PHYSICAL_DAMAGE.get());
            event.add(entity, BASIC_MAGIC_DAMAGE.get());
            event.add(entity, CANTOR_LIMIT.get());
        });
    }
}
