package ddist;

import java.io.Serializable;
import java.util.ArrayList;

public class VectorClock implements Comparable<VectorClock>, Serializable {
    ArrayList<Integer> vector = new ArrayList<>();

    public VectorClock(ArrayList<Integer> vector) {
        this.vector = vector;
    }

    public VectorClock() {
        ArrayList<Integer> temp = new ArrayList<>();
        temp.add(0);
        this.vector = temp;
    }

    public ArrayList<Integer> getVector() {
        return vector;
    }

    @Override
    public int compareTo(VectorClock o) {
        boolean jLargerThani = false;
        boolean iLargerThanj = false;
        for(Integer i : vector) {
            for(Integer j : o.getVector()) {
                if(j>i) jLargerThani = true;
                if(i>j) iLargerThanj = true;
            }
        }
        if(jLargerThani && iLargerThanj) return 0;
        if(jLargerThani && !iLargerThanj) return -1;
        if(!jLargerThani && iLargerThanj) return 1;
        if(!jLargerThani && !iLargerThanj) return 0;
        else return 0;
    }

    public void add(int i) {
        vector.add(i);
    }

    public int size() {
        return vector.size();
    }

    public int get(int index) {
        if(index >= vector.size()) return 0;
        return vector.get(index);
    }

    public void set(int index, int value) {
        try {
            vector.set(index, value);
        }catch (IndexOutOfBoundsException e) {
            vector.add(0);
        }
    }

    public void addPeer() {
        vector.add(0);
    }

    public void removePeer(int id) {
        vector.remove(id);
    }
}
