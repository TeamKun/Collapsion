package net.kunmc.lab.collapsion;

public class QueueData {
    private final Object object;
    private int commandTick;
    private Queues.Command name;
    public QueueData(Queues.Command name, int commandTick, Object object){
        this.commandTick= commandTick;
        this.name = name;
        this.object = object;
    }

    public Object getObject() {
        return object;
    }

    public Queues.Command getCommand() {
        return name;
    }

    public int getCommandTick() {
        return commandTick;
    }

    public boolean isCommandEqual(Queues.Command name){
        return this.name == name;
    }
}
