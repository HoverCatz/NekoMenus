package n.e.k.o.menus;

import n.e.k.o.menus.utils.StringColorUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Consumer;

public class Menu {

    public final String id;
    public String title;
    public int height;

    public Map<Integer, MenuItem> items;
    public Map<Integer, MenuItem> emptyItems;

    public Menu(String id) {
        this.id = id;
        this.items = new HashMap<>();
        this.emptyItems = new HashMap<>();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setHeight(int height) {
        if (height < 1) height = 1; else
        if (height > 6) height = 6;
        this.height = height;
    }
    public void addItem(MenuItem item) {
        this.items.put(item.slot, item);
    }

    public void addEmptyItem(MenuItem item) {
        this.emptyItems.put(item.slot, item);
    }

    public INamedContainerProvider build() {
        Inventory inventory = new Inventory(9 * height);
        for (int slot = 0, emptySlot = 0; slot < 9 * height; slot++) {
            MenuItem guiItem = items.getOrDefault(slot, emptyItems.getOrDefault((emptySlot++) % emptyItems.size(), null));
            if (guiItem == null || guiItem.item == null)
                continue;
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(guiItem.item));
            ItemStack stack = new ItemStack(item, guiItem.amount);
            stack.setDisplayName(StringColorUtils.getColoredString(guiItem.name));
            if (!guiItem.lore.isEmpty()) {
                CompoundNBT display = stack.getOrCreateChildTag("display");
                ListNBT lore = new ListNBT();
                for (String str : guiItem.lore) {
                    IFormattableTextComponent colored = StringColorUtils.getColoredString(str);
                    String json = ITextComponent.Serializer.toJson(colored);
                    StringNBT nbt = StringNBT.valueOf(json);
                    lore.add(nbt);
                }
                display.put("Lore", lore);
            }
            inventory.setInventorySlotContents(slot, stack);
        }
        final Menu thisMenu = this;
        return new INamedContainerProvider() {
            @Override
            public Container createMenu(int windowId, @Nonnull PlayerInventory playerInventory, @Nonnull PlayerEntity player) {
                ContainerType<?> containerType;
                switch (height) {
                    case 1:
                        containerType = ContainerType.GENERIC_9X1;
                        break;
                    case 2:
                        containerType = ContainerType.GENERIC_9X2;
                        break;
                    case 3:
                    default:
                        containerType = ContainerType.GENERIC_9X3;
                        break;
                    case 4:
                        containerType = ContainerType.GENERIC_9X4;
                        break;
                    case 5:
                        containerType = ContainerType.GENERIC_9X5;
                        break;
                    case 6:
                        containerType = ContainerType.GENERIC_9X6;
                }
                return new CustomChestContainer(containerType, windowId, playerInventory, inventory, height, items, thisMenu);
            }
            @Nonnull
            @Override
            public ITextComponent getDisplayName() {
                return StringColorUtils.getColoredString(title);
            }
        };
    }

    public void open(ServerPlayerEntity player) {
        NekoMenus.runLater(() -> NetworkHooks.openGui(player, build()));
    }

    public static void open(Menu menu, ServerPlayerEntity player) {
        NekoMenus.runLater(() -> NetworkHooks.openGui(player, menu.build()));
    }

    public static void open(INamedContainerProvider menu, ServerPlayerEntity player) {
        NekoMenus.runLater(() -> NetworkHooks.openGui(player, menu));
    }

    public String print() {
        return "Menu '" + id + "' { Title = '" + title + "', Height = " + height + ", Items = " + items.size() + " }";
    }

    @Override
    public String toString() {
        return print();
    }

    public void testItems() {
        Map<String, Item> itemCache = new HashMap<>();
        Consumer<MenuItem> c = item -> {
            if (item.item == null)
                return;
            String itemId = item.item.toLowerCase();
            Item _item;
            if (itemCache.containsKey(itemId))
                _item = itemCache.get(itemId);
            else {
                _item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
                itemCache.put(itemId, _item);
            }
            if (_item == null)
                System.err.println("Didn't find item by name: '" + item.item + "' ('" + itemId + "') in menu (id = '" + id + "') at slot '" + item.slot + "'. The menu may display the wrong item.");
        };
        items.values().forEach(c);
        emptyItems.values().forEach(c);
        itemCache.clear();
    }

}
