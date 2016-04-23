package sidben.redstonejukebox.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import sidben.redstonejukebox.ModRedstoneJukebox;
import sidben.redstonejukebox.helper.RecordInfo;
import sidben.redstonejukebox.init.MyItems;
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.common.network.simpleimpl.IMessage;


/**
 * Represents the full list of record trades for a specific villager.
 * 
 */
public class RecordTradingFullListMessage implements IMessage
{

    // ---------------------------------------------
    // Fields
    // ---------------------------------------------
    private static final byte  TYPE_SELLING = 1;
    private static final byte  TYPE_BUYING  = 2;

    private MerchantRecipeList tradeList;



    // ---------------------------------------------
    // Methods
    // ---------------------------------------------

    public RecordTradingFullListMessage() {
    }

    public RecordTradingFullListMessage(MerchantRecipeList list) {
        this.tradeList = list;
    }



    // Reads the packet
    @SuppressWarnings("unchecked")
    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.tradeList = new MerchantRecipeList();
        final int listSize = buf.readShort();


        // Loop to parse all trades
        for (short i = 0; i < listSize; i++) {
            final byte tradeType = buf.readByte();                  // Trade type
            final int recordInfoId = buf.readInt();                 // Record the villager is buying / selling
            final short emeraldPrice = buf.readShort();             // Price in emeralds
            final int recipeUses = buf.readInt();                   // Amount of times the trade was used
            final int recipeMaxUses = buf.readInt();                // Maximum amount of times the trade can be used


            if (recordInfoId > -1 && emeraldPrice > 0) {
                MerchantRecipe recipe = null;
                final RecordInfo recordInfo = ModRedstoneJukebox.instance.getRecordInfoManager().getRecordInfoFromId(recordInfoId);
                final Item recordItem = Item.getItemById(recordInfo.recordItemId);

                final ItemStack emptyDisc = new ItemStack(MyItems.recordBlank, 1);
                final ItemStack musicDisc = new ItemStack(recordItem, 1, recordInfo.recordItemDamage);
                final ItemStack emeralds = new ItemStack(Items.emerald, emeraldPrice);


                // Create the trade
                if (tradeType == TYPE_BUYING) {
                    recipe = new MerchantRecipe(musicDisc, emeralds);
                } else if (tradeType == TYPE_SELLING) {
                    recipe = new MerchantRecipe(emptyDisc, emeralds, musicDisc);
                }

                // Since the tradeUses variable is hard-coded on 7, manually reduces the amount of
                // times this trade can be used.
                if (recipeMaxUses != 7) {
                    recipe.func_82783_a(recipeMaxUses - 7);
                }

                // Adds the trade uses
                if (recipeUses > 0) {
                    for (int j = 0; j < recipeUses; j++) {
                        recipe.incrementToolUses();
                    }
                }


                // Adds to the list
                this.tradeList.add(recipe);
            }
        }
    }

    // Write the packet
    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeShort(this.tradeList.size());

        MerchantRecipe recipe;
        for (final Object obj : this.tradeList) {
            recipe = (MerchantRecipe) obj;

            final ItemStack slotBuy1 = recipe.getItemToBuy();
            final ItemStack slotBuy2 = recipe.getSecondItemToBuy();
            final ItemStack slotSell = recipe.getItemToSell();
            int recordInfoId = -1;

            // Checks the recipe type
            if (slotSell.getItem() == Items.emerald) {
                recordInfoId = ModRedstoneJukebox.instance.getRecordInfoManager().getRecordInfoIdFromItemStack(slotBuy1);

                if (recordInfoId > -1) {
                    // Villager is buying records
                    buf.writeByte(TYPE_BUYING);             // Trade type
                    buf.writeInt(recordInfoId);             // Record the villager is buying
                    buf.writeShort(slotSell.stackSize);     // Price in emeralds
                }

            } else {
                recordInfoId = ModRedstoneJukebox.instance.getRecordInfoManager().getRecordInfoIdFromItemStack(slotSell);

                if (recordInfoId > -1) {
                    // Villager is selling records
                    buf.writeByte(TYPE_SELLING);            // Trade type
                    buf.writeInt(recordInfoId);             // Record the villager is selling
                    buf.writeShort(slotBuy2.stackSize);     // Price in emeralds
                }

            }


            if (recordInfoId > -1) {
                // Adds the trade uses
                final Object hiddenMax = ObfuscationReflectionHelper.getPrivateValue(MerchantRecipe.class, recipe, "maxTradeUses", "field_82786_e");
                final Object hiddenUses = ObfuscationReflectionHelper.getPrivateValue(MerchantRecipe.class, recipe, "toolUses", "field_77400_d");
                final int recipeMaxUses = hiddenMax == null ? -1 : (int) hiddenMax;
                final int recipeUses = hiddenUses == null ? -1 : (int) hiddenUses;

                buf.writeInt(recipeUses);
                buf.writeInt(recipeMaxUses);
            }

        }
    }



    public void updateClientSideRecordStore()
    {
        ModRedstoneJukebox.instance.getRecordStoreHelper().clientSideCurrentStore = this.tradeList;
    }


    
    
    @Override
    public String toString()
    {
        // TODO: integrate with RecordStoreHelper.debugMerchantList   1) itemStackToString()   2) merchantRecipeToString   3) MerchantListToString()
        final StringBuilder r = new StringBuilder();

        if (this.tradeList != null && this.tradeList.size() > 0) {
            r.append("List size: ");
            r.append(this.tradeList.size());
            
            MerchantRecipe recipe;
            int cont = 0;
            for (final Object obj : this.tradeList) {
                recipe = (MerchantRecipe) obj;
                
                r.append("\n    #" + cont + ": [");
                r.append(this.itemStackToString(recipe.getItemToBuy()));
                r.append("] + [");
                r.append(this.itemStackToString(recipe.getSecondItemToBuy()));
                r.append("] = [");
                r.append(this.itemStackToString(recipe.getItemToSell()));
                r.append("]");
                
                cont++;
            }

        } else {
            r.append("List size: NULL");
            
        }
        

        return r.toString();
    }
    
    
    private String itemStackToString(ItemStack stack) {
        String value = "";

        if (stack != null) {
            Item auxItem = stack.getItem();
            value = stack.stackSize + "x " + auxItem.getClass().getSimpleName() + " " + Item.getIdFromItem(auxItem) + ":" + stack.getItemDamage();
        }
        
        return value;
    }
    

}
