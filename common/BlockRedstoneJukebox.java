package sidben.redstonejukebox.common;

import java.util.Random;

import sidben.redstonejukebox.ModRedstoneJukebox;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.asm.*;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.block.*;
import net.minecraft.block.material.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.*;
import net.minecraft.world.*;
import net.minecraftforge.client.MinecraftForgeClient;


public class BlockRedstoneJukebox extends BlockContainer {


	/*--------------------------------------------------------------------
		Constants and Variables
	--------------------------------------------------------------------*/

    private Random random = new Random();

    // True if this is an active jukebox, false if idle 
    private final boolean isActive;
    
    /**
     * This flag is used to prevent the jukebox inventory to be dropped upon block removal, is used internally when the
     * jukebox block changes from idle to active and vice-versa.
     */
    private static boolean keepMyInventory = false;

    
    
	
    /*--------------------------------------------------------------------
		Constructors
	--------------------------------------------------------------------*/

    public BlockRedstoneJukebox(int blockID, boolean active) {
		super(blockID, ModRedstoneJukebox.texJukeboxBottom, Material.wood);
        this.isActive = active;
	}

	@Override
	public TileEntity createNewTileEntity(World var1) {
		return new TileEntityRedstoneJukebox();
	}

    
    
    
	/*--------------------------------------------------------------------
		Parameters
	--------------------------------------------------------------------*/

    /**
     * Is this block (a) opaque and (b) a full 1m cube?  This determines whether or not to render the shared face of two
     * adjacent blocks and also whether the player can attach torches, redstone wire, etc to this block.
     */
    public boolean isOpaqueCube()
    {
        return false;
    }

    /**
     * Returns the ID of the items to drop on destruction.
     */
    public int idDropped(int par1, Random par2Random, int par3)
    {
        return ModRedstoneJukebox.redstoneJukeboxIdleID;
    }

    /**
     * only called by clickMiddleMouseButton , and passed to inventory.setCurrentItem (along with isCreative)
     */
    public int idPicked(World par1World, int par2, int par3, int par4)
    {
        return ModRedstoneJukebox.redstoneJukeboxIdleID;
    }


    
    
    
    
    
	/*--------------------------------------------------------------------
		Textures and Rendering
	--------------------------------------------------------------------*/
	
	@Override
	public String getTextureFile () {
		return CommonProxy.textureSheet;
	}
	
	
    /**
     * If this block doesn't render as an ordinary block it will return False (examples: signs, buttons, stairs, etc)
     */
    public boolean renderAsNormalBlock()
    {
        return false;
    }
    
    
    /**
     * The type of render function that is called for this block
     */
    public int getRenderType()
    {
        return ModRedstoneJukebox.redstoneJukeboxModelID;
    }

    
    /**
     * Retrieves the block texture to use based on the display side. Args: iBlockAccess, x, y, z, side
     */
    public int getBlockTexture(IBlockAccess access, int x, int y, int z, int side)
    {
		return this.getBlockTextureFromSide(side);
    }


    /**
     * Returns the block texture based on the side being looked at.  Args: side
     */
    public int getBlockTextureFromSide(int side)
    {
    	switch(side)
    	{
    	case 0:
			//--- bottom
            return ModRedstoneJukebox.texJukeboxBottom;

    	case 1:
			//--- top
			return ModRedstoneJukebox.texJukeboxTop;

    	case 7:
			//--- Extra texture (disc)
			return ModRedstoneJukebox.texJukeboxDisc;

    	default:
	        //--- sides
			if (this.isActive) { return ModRedstoneJukebox.texJukeboxSideOn; }
			return ModRedstoneJukebox.texJukeboxSideOff;
		}
    }
    
    
    /**
     * Returns which pass should this block be rendered on. 0 for solids and 1 for alpha
     */
    public int getRenderBlockPass()
    {
        return 0;
    }

	
	
	
	
    /*--------------------------------------------------------------------
		World Events
	--------------------------------------------------------------------*/

    /**
     * Called whenever the block is added into the world. Args: world, x, y, z
     */
    public void onBlockAdded(World par1World, int x, int y, int z)
    {
		System.out.println("    RedstoneJukebox.onBlockAdded");
		System.out.println("		side = " + FMLCommonHandler.instance().getEffectiveSide());

		
		super.onBlockAdded(par1World, x, y, z);
        //par1World.markBlockForUpdate(x, y, z);
	}


    /**
     * Called upon block activation (right click on the block.)
     */
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int i, float a, float b, float c)
    {
    	// Avoids opening the GUI if sneaking
    	TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
        if (tileEntity == null || player.isSneaking()) { return false; }

        
        //System.out.println("	BlockRedstoneJukebox.onBlockActivated");
    	player.openGui(ModRedstoneJukebox.instance, ModRedstoneJukebox.redstoneJukeboxGuiID, world, x, y, z);
    	return true;
    }

    
    /**
     * ejects contained items into the world, and notifies neighbours of an update, as appropriate
     */
    public void breakBlock(World par1World, int x, int y, int z, int par5, int par6)
    {

    	if (!keepMyInventory)
		{
            TileEntityRedstoneJukebox teJukebox = (TileEntityRedstoneJukebox)par1World.getBlockTileEntity(x, y, z);

            if (teJukebox != null)
            {
				// teJukebox.stopPlaying();		<--- This call may cause the block to be replaced again after broke, it calls the "updateJukeboxBlockState"
            	teJukebox.ejectAllAndStopPlaying(par1World, x, y, z);
            }
		}

        super.breakBlock(par1World, x, y, z, par5, par6);
    }    
    

    /**
     * Lets the block know when one of its neighbor changes. Doesn't know which neighbor changed (coordinates passed are
     * their own) Args: x, y, z, neighbor blockID
     */
    public void onNeighborBlockChange(World par1World, int x, int y, int z, int blockID)
    {
		/*
		System.out.println("    RedstoneJukebox.onNeighborBlockChange");
		System.out.println("    	isRemote = " + par1World.isRemote);
		System.out.println("    	par1World.isBlockIndirectlyProvidingPowerTo(x, y-1, z, 5) = " + par1World.isBlockIndirectlyProvidingPowerTo(x, y-1, z, 5));;
		*/



        if (!par1World.isRemote)
        {
			// Only activates if powered from below
			// 		isBlockIndirectlyProvidingPowerTo - Args: x, y, z, direction
            if (!par1World.isBlockIndirectlyProvidingPowerTo(x, y-1, z, 5))
            {
                stopPlaying(par1World, x, y, z);
                // don't think this is needed
                //par1World.scheduleBlockUpdate(x, y, z, this.blockID, this.tickRate());
            }
            else if (par1World.isBlockIndirectlyProvidingPowerTo(x, y-1, z, 5))
            {
                startPlaying(par1World, x, y, z);
                // don't think this is needed
                //par1World.scheduleBlockUpdate(x, y, z, this.blockID, this.tickRate());
            }
        }


		super.onNeighborBlockChange(par1World, x, y, z, blockID);
    }

    
    
    
    /*--------------------------------------------------------------------
		Custom World Events
	--------------------------------------------------------------------*/
	
    /**
     * Update which block ID the jukebox is using depending on whether or not it is playing
     */
    public static void updateJukeboxBlockState(boolean active, World world, int x, int y, int z)
    {
		System.out.println("	updateJukeboxBlockState");
		System.out.println("		side = " + FMLCommonHandler.instance().getEffectiveSide());

		
		TileEntity teJukebox = world.getBlockTileEntity(x, y, z);
        keepMyInventory = true;

        if (active)
        {
            world.setBlockWithNotify(x, y, z, ModRedstoneJukebox.redstoneJukeboxActiveID);
        }
        else
        {
            world.setBlockWithNotify(x, y, z, ModRedstoneJukebox.redstoneJukeboxIdleID);
        }

        keepMyInventory = false;


        if (teJukebox != null)
        {
            teJukebox.validate();
            world.setBlockTileEntity(x, y, z, teJukebox);
        }
    }

    

    
    

	/*--------------------------------------------------------------------
		Visual Effects
	--------------------------------------------------------------------*/

    /**
     * A randomly called display update to be able to add particles or other items for display
     */
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World par1World, int par2, int par3, int par4, Random par5Random)
    {
        if (this.isActive)
        {
			// redstone ore sparkles
			Random var5 = par1World.rand;
			double var6 = 0.0625D;

			for (int var8 = 0; var8 < 6; ++var8)
			{
				double var9 = (double)((float)par2 + var5.nextFloat());
				double var11 = (double)((float)par3 + var5.nextFloat());
				double var13 = (double)((float)par4 + var5.nextFloat());

				if (var8 == 0 && !par1World.isBlockOpaqueCube(par2, par3 + 1, par4))
				{
					var11 = (double)(par3 + 1) + var6;
				}

				if (var8 == 1 && !par1World.isBlockOpaqueCube(par2, par3 - 1, par4))
				{
					var11 = (double)(par3 + 0) - var6;
				}

				if (var8 == 2 && !par1World.isBlockOpaqueCube(par2, par3, par4 + 1))
				{
					var13 = (double)(par4 + 1) + var6;
				}

				if (var8 == 3 && !par1World.isBlockOpaqueCube(par2, par3, par4 - 1))
				{
					var13 = (double)(par4 + 0) - var6;
				}

				if (var8 == 4 && !par1World.isBlockOpaqueCube(par2 + 1, par3, par4))
				{
					var9 = (double)(par2 + 1) + var6;
				}

				if (var8 == 5 && !par1World.isBlockOpaqueCube(par2 - 1, par3, par4))
				{
					var9 = (double)(par2 + 0) - var6;
				}

				if (var9 < (double)par2 || var9 > (double)(par2 + 1) || var11 < 0.0D || var11 > (double)(par3 + 1) || var13 < (double)par4 || var13 > (double)(par4 + 1))
				{
					par1World.spawnParticle("reddust", var9, var11, var13, 0.0D, 0.0D, 0.0D);
				}
			}

        }
    }








	/*--------------------------------------------------------------------
		Redstone logic
	--------------------------------------------------------------------*/

    /**
     * Can this block provide power. Only wire currently seems to have this change based on its state.
     */
    public boolean canProvidePower()
    {
        return true;
    }

    /**
     * Returns true if the block is emitting indirect/weak redstone power on the specified side. If isBlockNormalCube
     * returns true, standard redstone propagation rules will apply instead and this will not be called. Args: World, X,
     * Y, Z, side
     */
    public boolean isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side)
    {
		/*
		System.out.println("");
		System.out.println("    RedstoneJukebox.isProvidingWeakPower(world, " + x + ", " + y + ", " + z + ", " + side + ")");
		System.out.println("		active = " + this.isActive);
		*/

		if (side == 0 || side == 1)
		{
			return false;
		}
		else
		{
			return this.isActive;
		}
    }

    /**
     * Returns true if the block is emitting direct/strong redstone power on the specified side. Args: World, X, Y, Z,
     * side
     */
    public boolean isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side)
    {
		/*
		System.out.println("");
		System.out.println("    RedstoneJukebox.isProvidingStrongPower(world, " + x + ", " + y + ", " + z + ", " + side + ")");
		System.out.println("		active = " + this.isActive);
		*/

		return false;

    }









	/*--------------------------------------------------------------------
		Custom Methods
	--------------------------------------------------------------------*/

	//-- Stop playing this jukebox
    private void stopPlaying(World world, int x, int y, int z)
    {
		/*
		System.out.println("    RedstoneJukebox.stopPlaying(world, " + x + ", " + y + ", " + z + ")");
		*/

        if (!world.isRemote)
        {
            TileEntityRedstoneJukebox teJukebox = (TileEntityRedstoneJukebox)world.getBlockTileEntity(x, y, z);
            if (teJukebox != null)
            {
				teJukebox.stopPlaying();
			}
		}
	}


	//-- Start playing this jukebox
    private void startPlaying(World world, int x, int y, int z)
    {
		/*
		System.out.println("    RedstoneJukebox.startPlaying(world, " + x + ", " + y + ", " + z + ")");
		*/


        if (!world.isRemote)
        {
            TileEntityRedstoneJukebox teJukebox = (TileEntityRedstoneJukebox)world.getBlockTileEntity(x, y, z);
            if (teJukebox != null)
            {
				teJukebox.startPlaying();
			}


		}

	}
    
}