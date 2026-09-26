package net.narutomod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Position-only recording. Deliberately has no inventory, damage, chakra or cooldown fields. */
public final class TemporalHistory {
    private final int maxAge;
    private final Deque<Frame> frames = new ArrayDeque<>();

    public TemporalHistory(int historyTicks) { maxAge = Math.max(1, Math.min(100, historyTicks)); }

    public static final class Frame {
        public final long tick;
        public final double x, y, z;
        public final float yaw, pitch;
        public Frame(long tick, double x, double y, double z, float yaw, float pitch) {
            this.tick=tick; this.x=x; this.y=y; this.z=z; this.yaw=yaw; this.pitch=pitch;
        }
        public boolean finite() {
            return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && Float.isFinite(yaw) && Float.isFinite(pitch);
        }
        public double distanceSquared(Frame other) {
            double dx=x-other.x,dy=y-other.y,dz=z-other.z;
            return dx*dx+dy*dy+dz*dz;
        }
    }

    public void add(Frame frame) {
        if (!frame.finite()) return;
        if (!frames.isEmpty() && frame.tick <= frames.getLast().tick) return;
        frames.addLast(frame);
        while (!frames.isEmpty() && frame.tick-frames.getFirst().tick > maxAge) frames.removeFirst();
    }

    public Frame oldest() { return frames.peekFirst(); }
    public Frame newest() { return frames.peekLast(); }
    public int size() { return frames.size(); }

    public boolean usable(long now, long deadline, double maxDistance) {
        if (frames.size()<2 || now>=deadline || newest().tick-oldest().tick<6) return false;
        return oldest().distanceSquared(newest())<=maxDistance*maxDistance;
    }

    /** Reverse chronological samples with both endpoints, bounded for one network effect. */
    public List<Frame> reverseSamples(int limit) {
        List<Frame> all=new ArrayList<>(frames), result=new ArrayList<>();
        int count=Math.min(Math.max(2,Math.min(20,limit)),all.size());
        if (count==0) return result;
        if (count==1) { result.add(all.get(0));return result; }
        for(int i=0;i<count;i++) result.add(all.get(all.size()-1-(int)Math.round((double)i*(all.size()-1)/(count-1))));
        return result;
    }
}
