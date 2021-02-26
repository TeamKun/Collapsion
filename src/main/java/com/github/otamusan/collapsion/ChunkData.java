package com.github.otamusan.collapsion;

import java.util.ArrayList;
import java.util.Optional;

public class ChunkData {
    private int lastUpdatedTick;
    private boolean exist;
    private ArrayList<Integer> list;
    public ChunkData(){
        lastUpdatedTick = 0;
        exist =false;
        list = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            list.add(i);
        }
    }

    public ArrayList<Integer> getList() {
        return list;
    }

    public void setList(ArrayList<Integer> list) {
        this.list = list;
    }

    public void setLastUpdatedTick(int lastUpdatedTick) {
        exist = true;
        this.lastUpdatedTick = lastUpdatedTick;
    }

    public Optional<Integer> getLastUpdatedTick() {
        if(exist) return Optional.of(lastUpdatedTick);
        return Optional.empty();
    }
}
