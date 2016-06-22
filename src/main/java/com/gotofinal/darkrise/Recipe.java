package com.gotofinal.darkrise;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.caversia.plugins.economy.model.CustomItem;
import com.caversia.plugins.economy.persistence.ItemsRepository;
import com.gotofinal.darkrise.core.Vault;
import com.gotofinal.darkrise.core.utils.DeserializationWorker;
import com.gotofinal.darkrise.core.utils.SerializationBuilder;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import org.diorite.utils.collections.maps.CaseInsensitiveMap;

public class Recipe implements ConfigurationSerializable
{
    protected final String                 name;
    protected final Collection<RecipeItem> pattern;
    protected final RecipeItem             result;
    protected final double                 price;

    public Recipe(final Map<String, Object> map)
    {
        final DeserializationWorker dw = DeserializationWorker.start(map);
        this.name = dw.getString("name");
        this.result = RecipeItem.fromConfigString(dw.getString("result"));
        this.pattern = dw.getStringList("pattern").stream().map(RecipeItem::fromConfigString).collect(Collectors.toList());
        this.price = dw.getDouble("price", 0);
    }

    public Recipe(final String name, final Collection<RecipeItem> pattern, final RecipeItem result, final double price)
    {
        this.name = name;
        this.pattern = new ArrayList<>(pattern);
        this.result = result;
        this.price = price;
    }

    public boolean isValid(final Collection<ItemStack> items, final Player p)
    {
        if (items.isEmpty())
        {
            return false;
        }
        if ((p != null) && ! p.hasPermission("crafting.recipe." + this.name) && ! p.hasPermission("crafting.recipes"))
        {
            return false;
        }
        if (! Vault.canPay(p, this.price))
        {
            return false;
        }
        final ItemsRepository ir = ItemsRepository.INSTANCE;
        final Map<String, Entry<CustomItem, Integer>> eqItems = new CaseInsensitiveMap<>(20);
        for (final ItemStack item : items)
        {
            final CustomItem customItem = ir.getItem(item).get();
            eqItems.put(customItem.getName(), new SimpleEntry<>(customItem, item.getAmount()));
        }
        final Map<String, Entry<CustomItem, Integer>> localPattern = new CaseInsensitiveMap<>(20);
        for (final RecipeItem recipeItem : this.pattern)
        {
            localPattern.put(recipeItem.getItemName(), new SimpleEntry<>(recipeItem.asCustomItem(), recipeItem.getAmount()));
        }
        for (final Iterator<Entry<String, Entry<CustomItem, Integer>>> it = localPattern.entrySet().iterator(); it.hasNext(); )
        {
            final Entry<String, Entry<CustomItem, Integer>> patternEntry = it.next();
            final Entry<CustomItem, Integer> patternEntryData = patternEntry.getValue();
            final Entry<CustomItem, Integer> eqEntry = eqItems.get(patternEntry.getKey());
            if (eqEntry == null)
            {
                return false;
            }
            final int eqAmount = eqEntry.getValue();
            final int patternAmount = patternEntryData.getValue();
            if (eqAmount < patternAmount)
            {
                return false;
            }
            if (eqAmount == patternAmount)
            {
                eqItems.remove(patternEntry.getKey());
            }
            final int rest = eqAmount - patternAmount;
            eqEntry.setValue(rest);
            it.remove();
        }
        return localPattern.isEmpty();
    }

    public RecipeItem getResult()
    {
        return this.result;
    }

    public String getName()
    {
        return this.name;
    }

    public double getPrice()
    {
        return this.price;
    }

    public Collection<RecipeItem> getPattern()
    {
        return this.pattern;
    }

    public Collection<ItemStack> getItemsToTake()
    {
        return this.pattern.stream().map(RecipeItem::getItemStack).collect(Collectors.toList());
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (! (o instanceof Recipe))
        {
            return false;
        }
        final Recipe recipe = (Recipe) o;
        return this.name.equals(recipe.name) && this.pattern.equals(recipe.pattern);
    }

    @Override
    public int hashCode()
    {
        int result = this.name.hashCode();
        result = (31 * result) + this.pattern.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString()).append("name", this.name).append("price", this.price).append("pattern", this.pattern).append("result", this.result).toString();
    }

    @Override
    public Map<String, Object> serialize()
    {
        return SerializationBuilder.start(4).append("name", this.name).append("result", this.result.toConfigString()).append("price", this.price).append("pattern", this.pattern.stream().map(RecipeItem::toConfigString).collect(Collectors.toList())).build();
    }
}
