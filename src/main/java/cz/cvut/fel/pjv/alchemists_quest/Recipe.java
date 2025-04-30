package cz.cvut.fel.pjv.alchemists_quest;

import java.util.HashMap;
import java.util.Map;

public class Recipe {
    private Map<String, Integer> requiredItems = new HashMap<>();
    private String resultItem;

    public Recipe(String[] requiredItems, String resultItem) {
        for (String item : requiredItems) {
            this.requiredItems.put(item, 1); // Количество каждого предмета по умолчанию 1
        }
        this.resultItem = resultItem;
    }

    public Map<String, Integer> getRequiredItems() {
        return requiredItems;
    }

    public String getResultItem() {
        return resultItem;
    }
}
