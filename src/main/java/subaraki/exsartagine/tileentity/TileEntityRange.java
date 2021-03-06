package subaraki.exsartagine.tileentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import subaraki.exsartagine.block.BlockRangeExtension;
import subaraki.exsartagine.block.ExSartagineBlocks;

public class TileEntityRange extends TileEntity implements ITickable {

	private final ItemStackHandler inventory = new ItemStackHandler(9);

	private List<BlockPos> connected = new ArrayList<BlockPos>();

	/**wether or not this range is fueling connected entries*/
	private boolean isCooking;
	/**how much 'cooktime' from the item inserted is left*/
	private int fuelTimer = 0;
	/**used in gui for image drawing*/
	private int maxFuelTimer = 0;

	@Override
	public void update() {
		//Decrease
		if(fuelTimer > 0)
			fuelTimer--;


		if(!world.isRemote){
			//look for fuel if we ran out is present
			if(fuelTimer == 0)
			{
				for(int i = 0 ; i < inventory.getSlots() ; i++)
				{
					ItemStack stack = inventory.getStackInSlot(i);
					if(!stack.isEmpty() && TileEntityFurnace.isItemFuel(stack))
					{
						maxFuelTimer = fuelTimer = TileEntityFurnace.getItemBurnTime(stack);
						isCooking = true;
						setRangeConnectionsCooking(true);
						//shrink after getting fuel timer, or when stack was 1, fueltimer cannot get timer from stack 0
						inventory.getStackInSlot(i).shrink(1);
						markDirty();
						//sync to client
						IBlockState state = world.getBlockState(getPos());
						world.notifyBlockUpdate(getPos(), state, state, 3);
						break;
					}
				}
			}

			// if no fuel was set and the tile is cooking
			if(fuelTimer == 0 && isCooking)
			{
				isCooking = false;
				setRangeConnectionsCooking(false);
				IBlockState state = world.getBlockState(getPos());
				world.notifyBlockUpdate(getPos(), state, state, 3);
				markDirty();
			}
		}
	}

	public boolean isFueled() {
		return fuelTimer > 0;
	}

	public int getFuelTimer() {
		return fuelTimer;
	}

	public int getMaxFuelTimer() {
		return maxFuelTimer;
	}

	public ItemStackHandler getInventory() {
		return inventory;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			return true;
		}
		return super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			return (T) inventory;
		}
		return super.getCapability(capability, facing);
	}

	
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		compound.setTag("inv", inventory.serializeNBT());

		compound.setBoolean("cooking", isCooking);
		compound.setInteger("fuel", fuelTimer);
		compound.setInteger("max", maxFuelTimer);

		NBTTagCompound connections = new NBTTagCompound();
		int slot = 0;
		for(BlockPos pos : connected)
		{
			connections.setLong(Integer.toString(slot), pos.toLong());
			slot++;
		}
		compound.setTag("connections", connections);

		return compound;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		inventory.deserializeNBT(compound.getCompoundTag("inv"));
		isCooking = compound.getBoolean("cooking");
		fuelTimer = compound.getInteger("fuel");
		maxFuelTimer = compound.getInteger("max");

		NBTTagCompound connections = compound.getCompoundTag("connections");
		for (int i = 0; i < 4; i++)
			if(connections.hasKey(String.valueOf(i)))
			{
				BlockPos pos = BlockPos.fromLong(connections.getLong(String.valueOf(i)));
				connected.add(pos);
			}
	}

	public boolean canConnect(){
		//if the last one is filled, the rest is too.
		return connected.isEmpty() || connected.size() < 4;
	}

	public void connect(TileEntityRangeExtension tere){
		if(canConnect())
			connected.add(tere.getPos());

		markDirty();
	}

	public void disconnect(BlockPos entry){
		if(connected.contains(entry))
		{
			List<BlockPos> copy = new ArrayList<BlockPos>();
			for(BlockPos saved : connected)
			{
				if(saved.equals(entry))
					break;
				copy.add(saved);
			}
			connected = copy;
		}
		markDirty();
	}

	public void setRangeConnectionsCooking(boolean setCooking){
		if(!connected.isEmpty())
		{
			//backup of connections. it seems to get whiped when updating the annex furnaces
			List<BlockPos> backup = connected;
			
			for(BlockPos posTere : connected)
			{
				TileEntity te = world.getTileEntity(posTere);
				if(te instanceof TileEntityRangeExtension){
					TileEntityRangeExtension tere = ((TileEntityRangeExtension)te);
					tere.setCooking(setCooking);
					//tere.markDirty();

					IBlockState state = world.getBlockState(posTere);

					if(setCooking)
					{
						IBlockState lit = ExSartagineBlocks.range_extension_lit.getDefaultState().
								withProperty(BlockRangeExtension.FACING, state.getValue(BlockRangeExtension.FACING)).
								withProperty(BlockRangeExtension.ENDBLOCK, state.getValue(BlockRangeExtension.ENDBLOCK));

						//connexions get whiped here for some reason :
						//TODO fix this
						world.setBlockState(posTere, lit);
						world.notifyBlockUpdate(posTere, state, lit, 3);
					}
					else
					{
						IBlockState unlit = ExSartagineBlocks.range_extension.getDefaultState().
								withProperty(BlockRangeExtension.FACING, state.getValue(BlockRangeExtension.FACING)).
								withProperty(BlockRangeExtension.ENDBLOCK, state.getValue(BlockRangeExtension.ENDBLOCK));

						world.setBlockState(posTere, unlit);
						world.notifyBlockUpdate(posTere, state, unlit, 3);
					}
					//revalidate te tileentity. furnace does the same after changing block state
					tere.validate();
					//make sure a copy gets back to the position
					world.setTileEntity(posTere, tere);
				}
			}
			
			//set saved connections back to tile entity
			connected = backup;
		}
	}

	/////////////////3 METHODS ABSOLUTELY NEEDED FOR CLIENT/SERVER SYNCING/////////////////////
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		this.writeToNBT(nbt);

		return new SPacketUpdateTileEntity(getPos(), 0, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		this.readFromNBT(pkt.getNbtCompound());
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		NBTTagCompound nbt =  super.getUpdateTag();
		writeToNBT(nbt);
		return nbt;
	}

	//calls readFromNbt by default. no need to add anything in here
	@Override
	public void handleUpdateTag(NBTTagCompound tag) {
		super.handleUpdateTag(tag);
	}
	////////////////////////////////////////////////////////////////////

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate)
	{
		return oldState.getBlock() != newSate.getBlock();
	}
}
