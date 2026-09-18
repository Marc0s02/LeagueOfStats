package com.marclw.lolstats.ingest;

import com.marclw.lolstats.model.Champion;
import com.marclw.lolstats.model.GameMode;
import com.marclw.lolstats.model.Item;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemRulesTest {

    @Test
    void summonersRiftItemMustBeInStoreOnMap11AndPurchasable() {
        Item item = baseItem();
        assertTrue(ItemRules.isSelectable(item, GameMode.SUMMONERS_RIFT, null));

        item.setInStore(false);
        assertFalse(ItemRules.isSelectable(item, GameMode.SUMMONERS_RIFT, null));

        item.setInStore(true);
        item.setPurchasable(false);
        assertFalse(ItemRules.isSelectable(item, GameMode.SUMMONERS_RIFT, null));

        item.setPurchasable(true);
        item.setMaps(Map.of("11", false));
        assertFalse(ItemRules.isSelectable(item, GameMode.SUMMONERS_RIFT, null));
    }

    @Test
    void championSpecificItemOnlyAppearsForThatChampion() {
        Item item = baseItem();
        item.setRequiredChampion("Gangplank");

        Champion gangplank = new Champion();
        gangplank.setId(41);
        gangplank.setName("Gangplank");

        Champion ahri = new Champion();
        ahri.setId(103);
        ahri.setName("Ahri");

        assertFalse(ItemRules.isSelectable(item, GameMode.SUMMONERS_RIFT, null));
        assertTrue(ItemRules.isSelectable(item, GameMode.SUMMONERS_RIFT, gangplank));
        assertFalse(ItemRules.isSelectable(item, GameMode.SUMMONERS_RIFT, ahri));
    }

    private Item baseItem() {
        Item item = new Item();
        item.setId(3031);
        item.setName("Example Item");
        item.setCost(1000);
        item.setInStore(true);
        item.setPurchasable(true);
        item.setDisplayInItemSets(true);
        item.setMaps(Map.of("11", true));
        return item;
    }
}
