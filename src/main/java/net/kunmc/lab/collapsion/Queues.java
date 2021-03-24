package net.kunmc.lab.collapsion;

import java.util.LinkedList;
import java.util.Optional;

public class Queues {
    public LinkedList<QueueData> list;
    public Queues(){
        list = new LinkedList<>();
    }

    public static class Command{
        public static final Command START = new Command();
        public static final Command SPEED = new Command();
        //public static final Command SKIP = new Command();
        public static final Command REGENERATE = new Command();
        public static final Command PAUSE = new Command();
    }

    public LinkedList<QueueData> getList() {
        return list;
    }

    public void addQueue(QueueData queue){
        list.addFirst(queue);
    }

    public QueueData getLatestQueue(){
        return list.getFirst();
    }

    public Optional<QueueData> getLatestQueue(Command name){
        return list.stream().filter(data -> data.isCommandEqual(name)).findFirst();
    }

    public Optional<QueueData> getLatestQueue(Command name, QueueData exclude){
        return list.stream().filter(data -> (data.isCommandEqual(name) && data != exclude)).findFirst();
    }

    public boolean shouldRegenerate(int pretick, int currnttick){
        Optional<QueueData> queue = getLatestQueue(Command.REGENERATE);
        if(!queue.isPresent()) return false;
        return queue.get().getCommandTick()>=pretick && queue.get().getCommandTick()<=currnttick;
    }
    public double getResumedSpeed(){
        double speed = (double) list.stream().filter(queueData -> queueData.isCommandEqual(Command.START) || queueData.isCommandEqual(Command.SPEED)).findFirst().get().getObject();
        return speed;
    }

    public boolean isStarted(){
        return !list.isEmpty();
        //return getLatestQueue(Command.START).isPresent();
    }

    public int getStartedTick(){
        for (QueueData queueData : list) {
            if(queueData.isCommandEqual(Command.START)) return queueData.getCommandTick();
        }
        return 0;
    }

    public Tuple<Integer,Integer> getTicks(final int pretick, final int currenttick){
        return new Tuple<>(getTick(pretick-getStartedTick()),getTick(currenttick-getStartedTick()));
    }

    public int getTick(final int tick){
        int pre = 0;
        int t=0;
        double s = 0;
        for (int i = 0; i < list.size(); i++) {
            QueueData queue = list.get(list.size() - i - 1);
            //t += getSectionTick(pre,queue.getCommandTick(), tick, (Double) queue.getObject());
            if(queue.getCommand()==Command.SPEED || queue.getCommand() == Command.START || queue.getCommand() == Command.PAUSE) {
                t += getSectionTick(pre - getStartedTick(), queue.getCommandTick() - getStartedTick(), tick, s);
                s= (Double) queue.getObject();
                pre = queue.getCommandTick();
            }else if(queue.getCommand() == Command.REGENERATE){
                t=0;
            }
        }
        t += getSectionTick(pre-getStartedTick(),tick, tick,s);
        return t;
    }
    public int getSectionTick(int prequeuetick, int queuetick, int currenttick, double speed) {
        int e = 0;
        if (currenttick >= queuetick) {
            e = queuetick;
        } else if (currenttick >= prequeuetick && currenttick < queuetick) {
            e = currenttick-prequeuetick;
        } else if (currenttick < prequeuetick) {
            e = 0;
        } else{
            throw new Error();
        }
        return (int) (((double) e) * speed);
    }
}
