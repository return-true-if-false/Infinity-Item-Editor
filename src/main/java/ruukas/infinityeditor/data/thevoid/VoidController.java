package ruukas.infinityeditor.data.thevoid;

import java.io.File;
import java.util.Objects;

import org.apache.logging.log4j.Logger;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import ruukas.infinityeditor.InfinityEditor;
import ruukas.infinityeditor.data.InfinityConfig;
import ruukas.infinityeditor.util.ItemStackUtil;

@SideOnly( Side.CLIENT )
public class VoidController
{
    public static final String VERSION = "0.2";
    private static final Logger LOGGER = InfinityEditor.logger;
    private final File dataFile;
    private final NonNullList<VoidElement> elementList = NonNullList.create();
    
    public VoidController(ItemStack stack) {
        this.dataFile = new File( InfinityEditor.dataDir.getAbsolutePath() + File.separatorChar + "void", stack.getItem().getRegistryName().toString().replace( ':', '.' ) + ".nbt" );
        
        this.read();
    }
    
    private VoidController(File dataFile) {
        this.dataFile = dataFile;
        
        this.read();
    }
    
    public void read()
    {
        if ( !dataFile.exists() )
        {
            return;
        }
        
        try
        {
            NBTTagCompound root = CompressedStreamTools.read( this.dataFile );
            
            if ( root == null || !root.hasKey( "elements" ) )
            {
                // TODO remove this in later version (backwards compatibility)
                if ( root != null && root.hasKey( "stacks" ) )
                {
                    for ( NBTBase tag : root.getTagList( "stacks", NBT.TAG_COMPOUND ) )
                    {
                        elementList.add( VoidElement.readFromNBT( (NBTTagCompound) tag ) );
                    }
                }
                return;
            }
            
            for ( NBTBase tag : root.getTagList( "elements", NBT.TAG_COMPOUND ) )
            {
                elementList.add( VoidElement.readFromNBT( (NBTTagCompound) tag ) );
            }
            
        }
        catch ( Exception exception )
        {
            LOGGER.error( "Failed to load void for: " + dataFile.getName(), exception);
        }
    }
    
    public void write()
    {
        try
        {
            NBTTagCompound root = new NBTTagCompound();
            root.setTag( "elements", new NBTTagList() );
            NBTTagList elements = root.getTagList( "elements", NBT.TAG_COMPOUND );

            for (VoidElement voidElement : elementList) {
                elements.appendTag(voidElement.writeToNBT(new NBTTagCompound()));
            }
            
            CompressedStreamTools.write( root, this.dataFile );
        }
        catch ( Exception exception )
        {
            LOGGER.error( "Failed to save void for: " + dataFile.getName(), exception);
        }
    }
    
    public void addItemStack( EntityPlayerSP player, ItemStack stack, String from )
    {
        
        if ( stack == null || stack.isEmpty() || !stack.hasTagCompound() || (stack.getItem() instanceof ItemMonsterPlacer && stack.getTagCompound().getKeySet().size() == 1) )
        {
            return;
        }
        
        stack.setCount( 1 );
        if ( stack.getItem().isDamageable() )
        {
            stack.setItemDamage( 0 );
        }
        
        for ( VoidElement e : elementList )
        {
            if ( ItemStackUtil.isSameStack( e.getStack(), stack ) )
            {
                if ( e.addUUID( from, true ) )
                {
                    synchronized ( InfinityEditor.dataDir )
                    {
                        write();
                    }
                }
                
                return;
            }
        }
        
        if ( InfinityConfig.voidAddNotification )
            player.sendMessage( new TextComponentString( "Added " ).appendSibling( stack.getTextComponent() ).appendText( " to Infinity Void." ) );
        
        VoidElement e = new VoidElement( stack );
        e.addUUID( from, false );
        elementList.add( e );
        
        synchronized ( InfinityEditor.dataDir )
        {
            write();
        }
    }
    
    public NonNullList<VoidElement> getElementList()
    {
        return elementList;
    }
    
    public static synchronized void loadVoidToList( NonNullList<ItemStack> list )
    {
        File dataFile = new File( InfinityEditor.dataDir.getAbsolutePath() + File.separatorChar + "void" );

        for (File file : Objects.requireNonNull(dataFile.listFiles())) {
            VoidController control;

            synchronized (InfinityEditor.dataDir) {
                control = new VoidController(file);
            }

            NonNullList<VoidElement> eList = control.getElementList();

            if (!eList.isEmpty()) {
                if (InfinityConfig.voidTabHideHeads && eList.get(0).getStack().getItem() instanceof ItemSkull) {
                    continue;
                }

                for (VoidElement voidElement : eList) {
                    list.add(voidElement.getStack());
                }
            }
        }
    }
}