package xyz.alexcrea.cuanvil.util;

import io.delilaheve.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import xyz.alexcrea.cuanvil.enchant.CAEnchantment;
import xyz.alexcrea.cuanvil.listener.PrepareAnvilListener;
import xyz.alexcrea.cuanvil.mock.AnvilViewMock;
import xyz.alexcrea.cuanvil.mock.EnchantedItemStackMock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AnvilFuseTestUtil {

    public static ItemStack prepareItem(@NotNull Material material,
                                        @NotNull List<CAEnchantment> enchantments,
                                        @NotNull List<Integer> level){
        Assertions.assertEquals(enchantments.size(), level.size());

        HashMap<CAEnchantment, Integer> enchantmentMap = new HashMap<>();
        for (int i = 0; i < enchantments.size(); i++) {
            enchantmentMap.put(enchantments.get(i), level.get(i));
        }

        ItemStack item = new EnchantedItemStackMock(material);
        ItemUtil.INSTANCE.setEnchantmentsUnsafe(item, enchantmentMap);

        return item;
    }


    public static ItemStack prepareItem(@NotNull Material material,
                                        @NotNull List<String> enchantmentNames,
                                        Integer... levels){
        List<CAEnchantment> enchantments = new ArrayList<>();

        for (String enchantmentName : enchantmentNames) {
            List<CAEnchantment> enchantmentList = CAEnchantment.getListByName(enchantmentName);
            Assertions.assertNotEquals(0, enchantmentList.size(),
                    "Could not find enchantment \"" + enchantmentName + "\"");

            enchantments.addAll(enchantmentList);
        }

        return prepareItem(material, enchantments, List.of(levels));
    }


    /*
     * Need to use that as it seems setting item in the inventory will not trigger the anvil click even
     *
     * Not the best for non-custom anvil plugins but work in the context of CA
     */
    public static void imitateAnvilUpdate(
            @NotNull HumanEntity player,
            @NotNull AnvilInventory anvil) {

        AnvilViewMock view = new AnvilViewMock(player, anvil);
        try {
            PrepareAnvilEvent event = new PrepareAnvilEvent(view, anvil.getItem(2));

            // Not ideal but possible and the easiest so why not
            new PrepareAnvilListener().anvilCombineCheck(event);
            anvil.setResult(event.getResult());
        } catch (Exception e){
            e.printStackTrace();
            Assertions.fail();
        }
    }

    public static void executeAnvilTest(
            @NotNull AnvilInventory anvil,
            @NotNull HumanEntity player,
            @NotNull AnvilFuseTestData data
            ){
        Assertions.assertEquals(player.getOpenInventory().getTopInventory(), anvil,
                "Openned inventory is not anvil");

        // Test with only the left item
        testPlacingItem(anvil, player,
                0, data.expectedPriceAfterLeftPlaced(),
                data.leftItem(), data.expectedAfterLeftPlaced());

        // Test with only the right item
        anvil.setItem(0, null); // We only want the right item. so we remove the left one
        testPlacingItem(anvil, player,
                1, data.expectedPriceAfterRightPlaced(),
                data.rightItem(), data.expectedAfterRightPlaced());

        // Test with both placed
        testPlacingItem(anvil, player,
                0, data.expectedPriceAfterBothPlaced(),
                data.leftItem(), data.expectedResult());

        // Sadly, can't currently test player click

    }

    @SuppressWarnings({"removal"})
    private static void testPlacingItem(
            @NotNull AnvilInventory anvil,
            @NotNull HumanEntity player,
            int slot,
            Integer expectedPrice,
            @Nullable ItemStack toPlace,
            @Nullable ItemStack expectedResult){
        anvil.setItem(slot, toPlace);
        AnvilFuseTestUtil.imitateAnvilUpdate(player, anvil);

        ItemStack result = anvil.getItem(2);
        assertEqual(result, expectedResult);
        assertPriceEqual(expectedPrice, anvil.getRepairCost());
    }

    public static void assertEqual(@Nullable ItemStack item1, @Nullable ItemStack item2) {
        if(isAir(item1)) Assertions.assertTrue(isAir(item2));
        else Assertions.assertEquals(item1, item2);

    }

    public static boolean isAir(@Nullable ItemStack item){
        return item == null || item.isEmpty();
    }

    public static void assertPriceEqual(Integer expectedPrice, int price){
        if(expectedPrice == null) return;
        Assertions.assertEquals(price, expectedPrice);
    }

}
