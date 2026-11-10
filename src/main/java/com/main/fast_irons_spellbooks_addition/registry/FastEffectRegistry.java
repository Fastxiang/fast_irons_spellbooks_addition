package com.main.fast_irons_spellbooks_addition.registry;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.effect.ArmorPenetrationEffect;
import com.main.fast_irons_spellbooks_addition.effect.BloodDebtEffect;
import com.main.fast_irons_spellbooks_addition.effect.RayOfFrostPursuitEffect;
import com.main.fast_irons_spellbooks_addition.effect.TwinSoulEffect;
import com.main.fast_irons_spellbooks_addition.effect.VoltStrikeRefreshEffect;
import com.main.fast_irons_spellbooks_addition.effect.WarCryEffect;
import com.main.fast_irons_spellbooks_addition.effect.WeakPointMarkEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FastEffectRegistry {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, FastIronsSpellbooksAddition.MODID);

    public static final RegistryObject<MobEffect> WAR_CRY =
            EFFECTS.register("war_cry", WarCryEffect::new);

    public static final RegistryObject<MobEffect> ARMOR_PENETRATION =
            EFFECTS.register("armor_penetration", ArmorPenetrationEffect::new);

    public static final RegistryObject<MobEffect> VOLT_STRIKE_REFRESH =
            EFFECTS.register("volt_strike_refresh", () -> new VoltStrikeRefreshEffect(MobEffectCategory.BENEFICIAL, 218138879));

    /** 猩红刺狱-血债：挂在目标身上的负面效果，负责倒计时显示与到期结算 */
    public static final RegistryObject<MobEffect> BLOOD_DEBT =
            EFFECTS.register("blood_debt", BloodDebtEffect::new);

    /** 烟火四射-双生：挂在施法者身上的窗口效果，HUD 可见，被消费后消失 */
    public static final RegistryObject<MobEffect> TWIN_SOUL =
            EFFECTS.register("twin_soul", TwinSoulEffect::new);

    /** 弱点标记：下一次枪械爆头伤害翻倍，命中后由 KJS 移除 */
    public static final RegistryObject<MobEffect> WEAK_POINT_MARK =
            EFFECTS.register("weak_point_mark", WeakPointMarkEffect::new);

    /** 冰霜射线-追击：挂在施法者身上的窗口效果，30 秒内造成伤害时追加一条冰霜射线 */
    public static final RegistryObject<MobEffect> RAY_OF_FROST_PURSUIT =
            EFFECTS.register("ray_of_frost_pursuit", RayOfFrostPursuitEffect::new);

    /** 大剑武装：状态期间左键普攻被大剑挥砍替代 */

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}