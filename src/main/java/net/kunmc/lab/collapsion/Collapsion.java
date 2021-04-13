package net.kunmc.lab.collapsion;

import net.minecraft.server.v1_15_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_15_R1.block.CapturedBlockState;
import org.bukkit.craftbukkit.v1_15_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_15_R1.block.data.CraftBlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Collapsion extends JavaPlugin implements Listener {
    public static Queues queues;
    public static Map<Long, ChunkData> chunkDataMap;
    public static Server server;
    public static Thread thread;
    public static Plugin plugin;
    public static Random random;
    public static final double defaultspeed = 0.01;
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this,this);
        thread = new Thread(getServer());
        thread.runTaskTimer(this, 1, 50);
        queues = new Queues();
        chunkDataMap = new HashMap<>();
        plugin = this;
        server = getServer();
        random = new Random();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!sender.isOp()){
            sender.sendMessage("no permissions");
            return false;
        }

        if(!command.getName().equals("col")) return false;
        String commandname = args[0];

        if(commandname.equals("start")) {
            if(queues.getLatestQueue(Queues.Command.START).isPresent()){
                sender.sendMessage("already started");
                return false;
            }
            if(args.length!=2) {
                double speed = defaultspeed;
                queues.addQueue(new QueueData(Queues.Command.START,server.getCurrentTick(), speed));
                Bukkit.getOnlinePlayers().forEach(player -> {
                    sender.sendMessage("Started at a speed of "+speed);
                });
                return false;
            }
            try {
                double speed = Double.parseDouble(args[1]);
                queues.addQueue(new QueueData(Queues.Command.START,server.getCurrentTick(), speed));
                Bukkit.getOnlinePlayers().forEach(player -> {
                    sender.sendMessage("Started at a speed of "+speed);
                });
            }catch (NumberFormatException e){
                sender.sendMessage("use double");
            }
        }else if(commandname.equals("speed")){
            if(!queues.getLatestQueue(Queues.Command.START).isPresent()){
                sender.sendMessage("not started");
                return false;
            }
            if(args.length!=2){
                sender.sendMessage("not enough args");
                return false;
            }
            try {
                double speed = Double.parseDouble(args[1]);
                queues.addQueue(new QueueData( Queues.Command.SPEED,server.getCurrentTick(), speed));
                Bukkit.getOnlinePlayers().forEach(player -> {
                    sender.sendMessage("speed changed to "+speed);
                });
            }catch (NumberFormatException e){
                e.printStackTrace();
                sender.sendMessage("use double");
            }


        }else if(commandname.equals("reset")){
            if(!queues.getLatestQueue(Queues.Command.START).isPresent()){
                sender.sendMessage("not started");
                return false;
            }
            queues = new Queues();
            chunkDataMap = new HashMap<>();
            sender.sendMessage("reseted!");
        }else if(commandname.equals("stop")) {
            if(!queues.getLatestQueue(Queues.Command.START).isPresent()){
                sender.sendMessage("not started");
                return false;
            }
            queues.addQueue(new QueueData( Queues.Command.PAUSE,server.getCurrentTick(), 0d));
            Bukkit.getOnlinePlayers().forEach(player -> {
                sender.sendMessage("stopped!");
            });
        }else if(commandname.equals("resume")) {
            if(!queues.getLatestQueue(Queues.Command.PAUSE).isPresent()){
                sender.sendMessage("not paused");
                return false;
            }
            queues.addQueue(new QueueData( Queues.Command.SPEED,server.getCurrentTick(), queues.getResumedSpeed()));
            Bukkit.getOnlinePlayers().forEach(player -> {
                sender.sendMessage("resumed!");
            });
        }
        return true;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if(!command.getName().equals("col"))
            return super.onTabComplete(sender, command, alias, args);
        switch (args.length){
            case 1:
                return Stream.of("start", "speed", "reset", "stop", "resume")
                    .filter(e -> e.startsWith(args[0]))
                    .collect(Collectors.toList());
            case 2:
                switch (args[0]){
                    case "start":
                        return Collections.singletonList("0.01");
                    case "speed":
                        return Collections.singletonList("0.00");
                    default:
                        return Collections.EMPTY_LIST;
                }
            default:
                return Collections.EMPTY_LIST;
        }

        /*if(command.getName().equals("col")){
            if(args.length ==2) {
                if (args[0].equals("start") && args[1].length() == 0) {
                    return Collections.singletonList(String.valueOf(defaultspeed));
                }
                if (args[0].equals("speed") && args[1].length() == 0) {
                    return Collections.singletonList("0.00");
                }
            }
            if(args[0].length()==0){
                return Arrays.asList("start", "speed", "reset", "updateTick", "stop", "resume");
            }else{
                return Stream.of("start", "speed", "reset", "updateTick", "stop", "resume").filter(e -> e.startsWith(args[0])).collect(Collectors.toList());
            }
        }else {
            return super.onTabComplete(sender, command, alias, args);
        }*/
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onLoadChunk(ChunkLoadEvent event){
        if(!queues.isStarted()) return;
        Chunk chunk = event.getChunk();
        if(event.isNewChunk()) return;
        //updateChunk(chunk);
        new UpdateChunk(chunk).runTaskAsynchronously(plugin);
    }
    public static class UpdateChunk extends BukkitRunnable {
        private Chunk chunk;
        public UpdateChunk (Chunk chunk){
            this.chunk = chunk;
        }
        @Override
        public void run() {
            try {
                //new BukkitRunnable() {
                //    @Override
                //    public void run() {
                        updateChunk(chunk);
                //    }
                //}.runTask(plugin);
                //updateChunk(chunk);
            }catch (Exception e){
                //e.printStackTrace();
            }
        }
    }
    @EventHandler
    public void onCreatedChunk(ChunkPopulateEvent event){
        if(!queues.isStarted()) return;
        Chunk chunk = event.getChunk();
        //updateChunk(chunk);
        new UpdateChunk(chunk).runTaskAsynchronously(plugin);
        //new UpdateChunk(chunk).runTaskAsynchronously(this);
    }

    public static ChunkData createOrGetChunkData(Chunk chunk){
        ChunkData chunkData;
        if (chunkDataMap.containsKey(chunk.getChunkKey())){
            chunkData = chunkDataMap.get(chunk.getChunkKey());
        }else {
            chunkData = new ChunkData();
            chunkDataMap.put(chunk.getChunkKey(), chunkData);
        }
        return chunkData;
    }

    public static void update(Server server){
        server.getWorlds().stream().flatMap(world -> Arrays.stream(world.getLoadedChunks())).forEach(Collapsion::updateChunk);
    }

    public static void updateChunk(Chunk chunk){
        ChunkData data = createOrGetChunkData(chunk);

        Tuple<Integer,Integer> ticks = queues.getTicks(data.getLastUpdatedTick().orElse(queues.getStartedTick()),server.getCurrentTick());
        updateChunk(ticks.getLeft(),ticks.getRight(),chunk);

        data.setLastUpdatedTick(server.getCurrentTick());
    }

    public static Tuple<Integer,Integer> getloc(Chunk chunk){
        Random rnd = new Random(chunk.getChunkKey());
        ArrayList<Integer> data = createOrGetChunkData(chunk).getList();
        int n = rnd.nextInt(data.size());
        int i=data.get(n);
        data.remove(n);
        int x = i%16;
        int z = (int) Math.floor(i/16);
        return new Tuple<>(x,z);
    }

    public static boolean setTypeAndDataWithoutLight(World world, BlockPosition blockposition, IBlockData iblockdata, int i) {
        if (world.captureTreeGeneration) {
            CapturedBlockState blockstate = (CapturedBlockState)world.capturedBlockStates.get(blockposition);
            if (blockstate == null) {
                blockstate = CapturedBlockState.getTreeBlockState(world, blockposition, i);
                world.capturedBlockStates.put(blockposition.immutableCopy(), blockstate);
            }

            blockstate.setData(iblockdata);
            return true;
        } else if (world.isOutsideWorld(blockposition)) {
            return false;
        } else if (!world.isClientSide && world.worldData.getType() == WorldType.DEBUG_ALL_BLOCK_STATES) {
            return false;
        } else {
            net.minecraft.server.v1_15_R1.Chunk chunk = world.getChunkAtWorldCoords(blockposition);
            net.minecraft.server.v1_15_R1.Block block = iblockdata.getBlock();
            boolean captured = false;
            if (world.captureBlockStates && !world.capturedBlockStates.containsKey(blockposition)) {
                CapturedBlockState blockstate = CapturedBlockState.getBlockState(world, blockposition, i);
                world.capturedBlockStates.put(blockposition.immutableCopy(), blockstate);
                captured = true;
            }

            IBlockData iblockdata1 = chunk.setType(blockposition, iblockdata, (i & 64) != 0, (i & 1024) == 0);
            if (iblockdata1 == null) {
                if (world.captureBlockStates && captured) {
                    world.capturedBlockStates.remove(blockposition);
                }

                return false;
            } else {
                IBlockData iblockdata2 = world.getType(blockposition);
                if (iblockdata2 != iblockdata1 && (iblockdata2.b(world, blockposition) != iblockdata1.b(world, blockposition) || iblockdata2.h() != iblockdata1.h() || iblockdata2.g() || iblockdata1.g())) {
                    //world.getMethodProfiler().enter("queueCheckLight");
                    //world.getChunkProvider().getLightEngine().a(blockposition);
                    //world.getMethodProfiler().exit();
                }

                if (!world.captureBlockStates) {
                    try {
                        world.notifyAndUpdatePhysics(blockposition, chunk, iblockdata1, iblockdata, iblockdata2, i);
                    } catch (StackOverflowError var10) {
                        world.lastPhysicsProblem = new BlockPosition(blockposition);
                        throw var10;

                    }
                }

                return true;
            }
        }
    }

    public static boolean setTypeAndData(CraftBlock block, IBlockData blockData, boolean applyPhysics) {
        GeneratorAccess world = null;
        try {
            Field fworld = CraftBlock.class.getDeclaredField("world");
            fworld.setAccessible(true);
            world= (GeneratorAccess) fworld.get(block);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        if (!blockData.isAir() && blockData.getBlock() instanceof BlockTileEntity && blockData.getBlock() != block.getNMS().getBlock()) {
            if (block.getWorld() instanceof net.minecraft.server.v1_15_R1.World) {
                ((net.minecraft.server.v1_15_R1.World) block.getWorld()).removeTileEntity(block.getPosition());
            } else {
                //block.getWorld().setTypeAndData(block.getPosition(), Blocks.AIR.getBlockData(), 0);
                setTypeAndDataWithoutLight((World) block.getWorld(), block.getPosition(), Blocks.AIR.getBlockData(),0);
            }
        }

        if (applyPhysics) {
            return setTypeAndDataWithoutLight((World) block.getWorld(),block.getPosition(),blockData, 3);
        } else {
            IBlockData old = world.getType(block.getPosition());
            //boolean success = world.setTypeAndData(block.getPosition(), blockData, 1042);
            boolean success = setTypeAndDataWithoutLight((World) world, block.getPosition(),blockData,1042);
            if (success) {
                world.getMinecraftWorld().notify(block.getPosition(), old, blockData, 3);
            }

            return success;
        }
    }

    public static void updateChunk(int preTick, int currentTick, Chunk chunk){
        //int a = preTick+60;
        //int b = currentTick+60;
        new BukkitRunnable() {
            @Override
            public void run() {
                int a = currentTick-preTick;
                for (int i = 0; i < a; i++) {
                    ArrayList<Integer> data = createOrGetChunkData(chunk).getList();
                    if(data.size() <= 0)return;
                    Tuple<Integer,Integer> p = getloc(chunk);
                    for (int y = 0; y < 256; y++) {
                        Block block = chunk.getBlock(p.getLeft(), y, p.getRight());
                        //block.setType(org.bukkit.Material.AIR, false);
                        setTypeAndData((CraftBlock) block,((CraftBlockData) org.bukkit.Material.AIR.createBlockData()).getState() ,false);


                    }
                }
            }
        }.runTaskLater(plugin,random.nextInt(20));

    }

    public static class Thread extends BukkitRunnable {
        Server server;
        public Thread(Server server){
            this.server = server;
        }

        @Override
        public void run(){
            if(!queues.isStarted()) return;
            update(server);

        }
    }

}
