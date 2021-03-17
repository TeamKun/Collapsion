package net.kunmc.lab.collapsion;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public final class Collapsion extends JavaPlugin implements Listener {
    public static Queues queues;
    public static Map<Long, ChunkData> chunkDataMap;
    public static Server server;
    public static Thread thread;
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this,this);
        thread = new Thread(getServer());
        thread.runTaskTimer(this, 1, 50);
        queues = new Queues();
        chunkDataMap = new HashMap<>();
        server = getServer();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!sender.isOp()){
            sender.sendMessage("no permissions");
            return false;
        }

        if(command.getName().equals("col_start")) {
            if(queues.getLatestQueue(Queues.Command.START).isPresent()){
                sender.sendMessage("already started");
                return false;
            }
            if(args.length!=1) {
                sender.sendMessage("not enough args");
                return false;
            }
            try {
                double speed = Double.parseDouble(args[0]);
                queues.addQueue(new QueueData(Queues.Command.START,server.getCurrentTick(), speed));
                sender.sendMessage("start!");
            }catch (NumberFormatException e){
                sender.sendMessage("use double");
            }
        }else if(command.getName().equals("col_speed")){
            if(!queues.getLatestQueue(Queues.Command.START).isPresent()){
                sender.sendMessage("not started");
                return false;
            }
            if(args.length!=1){
                sender.sendMessage("not enough args");
                return false;
            }
            try {
                double speed = Double.parseDouble(args[0]);
                queues.addQueue(new QueueData( Queues.Command.SPEED,server.getCurrentTick(), speed));
                sender.sendMessage("changed!");
            }catch (NumberFormatException e){
                sender.sendMessage("use double");
            }


        }else if(command.getName().equals("col_reset")){
            if(!queues.getLatestQueue(Queues.Command.START).isPresent()){
                sender.sendMessage("not started");
                return false;
            }
            queues = new Queues();
            chunkDataMap = new HashMap<>();
            sender.sendMessage("reseted!");
        }else if(command.getName().equals("col_updateTick")){
            if(args.length!=1){
                sender.sendMessage("not enough args");
                return false;
            }
            try {
                int speed = Integer.parseInt(args[0]);
                thread.cancel();
                thread = new Thread(getServer());
                thread.runTaskTimer(this, 1, speed);
                sender.sendMessage("changed the update interval");
            }catch (NumberFormatException e){
                sender.sendMessage("use integer");
            }
        }/*else if(command.getName().equals("col_skip")){
            if(!queues.getLatestQueue(Queues.Command.START).isPresent()){
                sender.sendMessage("not started");
                return false;
            }
            if(args.length!=1){
                sender.sendMessage("not enough args");
                return false;
            }
            try {
                int tick = Integer.parseInt(args[0]);
                queues.addQueue(new QueueData( Queues.Command.SKIP,server.getCurrentTick(), tick));
                sender.sendMessage("skipped!");
            }catch (NumberFormatException e){
                sender.sendMessage("use integer");
            }
        }*/
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if(command.getName().equals("col_start") && args[0].length()==0) {
            return Collections.singletonList("0.02");
        }
        if(command.getName().equals("col_speed") && args[0].length()==0) {
            return Collections.singletonList("0.00");
        }
        return super.onTabComplete(sender, command, alias, args);
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onLoadChunk(ChunkLoadEvent event){
        if(!queues.isStarted()) return;
        Chunk chunk = event.getChunk();
        if(event.isNewChunk()) return;
        updateChunk(chunk);
        /*Pair<Integer,Integer> ticks = queues.getTicks(createOrGetChunkData(chunk).getLastUpdatedTick().orElse(queues.getStartedTick()),server.getCurrentTick());
        if(ticks.getValue()-ticks.getKey()>=255){
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 256; y++) {
                        Block block = chunk.getBlock(x, y, z);
                        block.setType(Material.AIR, false);
                    }
                }
            }
            return;
        }
        new UpdateChunk(chunk).runTaskAsynchronously(this);*/
    }
    public static class UpdateChunk extends BukkitRunnable {
        private Chunk chunk;
        public UpdateChunk (Chunk chunk){
            this.chunk = chunk;
        }
        @Override
        public void run() {
            try {
                updateChunk(chunk);
            }catch (Exception e){
                //e.printStackTrace();
            }
        }
    }
    @EventHandler
    public void onCreatedChunk(ChunkPopulateEvent event){
        if(!queues.isStarted()) return;
        Chunk chunk = event.getChunk();
        updateChunk(chunk);
        /*Pair<Integer,Integer> ticks = queues.getTicks(createOrGetChunkData(chunk).getLastUpdatedTick().orElse(queues.getStartedTick()),server.getCurrentTick());
        if(ticks.getValue()-ticks.getKey()>=255){
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {

                    for (int y = 0; y < 256; y++) {
                        Block block = chunk.getBlock(x, y, z);
                        block.setType(Material.AIR, false);
                    }
                }
            }
            return;
        }
        new UpdateChunk(chunk).runTaskAsynchronously(this);*/
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

        /*QueueData queue = queues.getLatestQueue();
        if(queue.getCommand()== Queues.Command.START) {
            int currentTick = server.getCurrentTick();
            int startTick = queue.getCommandTick();
            int preTick = data.getLastUpdatedTick().orElse(startTick);
            System.out.println(preTick+":"+startTick);
            updateChunk(preTick-startTick, currentTick-startTick, chunk);
            data.setLastUpdatedTick(currentTick);
        }else if(queue.getCommand()== Queues.Command.PAUSE){
            int currentTick = server.getCurrentTick();
            int startTick = queue.getCommandTick();
            int preTick = data.getLastUpdatedTick().orElse(startTick);
            System.out.println(preTick+"l"+startTick);
            updateChunk(preTick-startTick, startTick, chunk);
            data.setLastUpdatedTick(currentTick);
        }else if(queue.getCommand()== Queues.Command.RESUME){
            int currentTick = server.getCurrentTick();
            int startTick = queues.getList().stream().filter(queueData -> queueData != queue).filter(queueData -> queueData.isCommandEqual(Queues.Command.START) || queueData.isCommandEqual(Queues.Command.RESUME)).findFirst().get().getCommandTick();

            int preTick = data.getLastUpdatedTick().orElse(startTick);
            updateChunk(startTick, currentTick-startTick, chunk);
            data.setLastUpdatedTick(currentTick);
        }*/

        Tuple<Integer,Integer> ticks = queues.getTicks(data.getLastUpdatedTick().orElse(queues.getStartedTick()),server.getCurrentTick());
        //if(queues.shouldRegenerate(ticks.getKey(), ticks.getValue())){
        //    regenerateChunk(chunk);
        //}
        //System.out.println(data.getLastUpdatedTick()+":"+ticks);

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

    public static void updateChunk(int preTick, int currentTick, Chunk chunk){
        //int a = preTick+60;
        //int b = currentTick+60;

        int a = currentTick-preTick;
        for (int i = 0; i < a; i++) {
            ArrayList<Integer> data = createOrGetChunkData(chunk).getList();
            if(data.size() <= 0)return;
            Tuple<Integer,Integer> p = getloc(chunk);
            for (int y = 0; y < 256; y++) {
                Block block = chunk.getBlock(p.getLeft(), y, p.getRight());
                block.setType(Material.AIR, false);
            }
        }
        /*int s = (int) ((Math.sin(preTick/60)*20)+60);
        int e = (int) ((Math.sin(currentTick/60)*20)+60);
        boolean f = false;
        System.out.println(Math.sin(currentTick/30));
        System.out.println(s+":"+e);

        if(s > e) {
            int buf = e;
            e = s;
            s = buf;
            f = true;
        }*/

        //if (b >= 255) b=255;

        /*for (int y = s; y < e; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    Block block = chunk.getBlock(x,y,z);
                    if(f && block.getType() == Material.WATER){
                        block.setType(Material.AIR,false);
                        continue;
                    }
                    if(block.getType() == Material.AIR || block.getType()==Material.CAVE_AIR) {
                        block.setType(Material.WATER,false);
                    }
                }
            }
        }*/
        /*for (int y = a; y < b; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    Block block = chunk.getBlock(x,y,z);
                    if(block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR) {
                        block.setType(Material.WATER,false);
                    }
                }
            }
        }*/

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
