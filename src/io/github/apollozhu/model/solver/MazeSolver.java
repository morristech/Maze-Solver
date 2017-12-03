package io.github.apollozhu.model.solver;

import io.github.apollozhu.model.MazeCoder;

import javax.swing.event.EventListenerList;
import java.util.EventListener;
import java.util.function.Consumer;

/**
 * @author ApolloZhu, Pd. 1
 */
public abstract class MazeSolver {

    private final EventListenerList list = new EventListenerList();
    private MazeCoder.Block[][] grid;

    public final boolean start(MazeCoder.Block[][] input,
                               int r, int c, int tR, int tC) {
        setGrid(input);
        if (get(r, c) == MazeCoder.Block.WALL || get(tR, tC) == MazeCoder.Block.WALL) return false;
        set(r, c, MazeCoder.Block.EMPTY);
        set(tR, tC, MazeCoder.Block.EMPTY);
        return start(r, c, tR, tC);
    }

    protected abstract boolean start(int r, int c, int tR, int tC);

    public void stop() {
        stop(false);
    }

    public void stop(boolean hasPath) {
        forEachListener(l -> l.ended(hasPath, grid));
    }

    public MazeCoder.Block[][] getGrid() {
        return grid;
    }

    public void setGrid(MazeCoder.Block[][] grid) {
        this.grid = grid;
    }

    protected MazeCoder.Block get(Loc loc) {
        return get(loc.getR(), loc.getC());
    }

    protected MazeCoder.Block get(int x, int y) {
        try {
            return grid[x][y];
        } catch (Exception e) {
            return MazeCoder.Block.WALL;
        }
    }

    protected void set(Loc loc, MazeCoder.Block block) {
        set(loc.getR(), loc.getC(), block);
    }

    protected void set(int x, int y, MazeCoder.Block block) {
        try {
            grid[x][y] = block;
        } catch (Exception e) {
        }
    }

    public void addEventListener(MSEventListener l) {
        list.add(MSEventListener.class, l);
    }

    public void removeEventListener(MSEventListener l) {
        list.remove(MSEventListener.class, l);
    }

    protected void forEachListener(Consumer<MSEventListener> consumer) {
        for (MSEventListener l : list.getListeners(MSEventListener.class))
            consumer.accept(l);
    }

    public enum Type {
        RECURSIVE, STACK, DFS, BFS;

        Class associatedClass() {
            switch (this) {
                case RECURSIVE:
                    return RecursiveMazeSolver.class;
                case STACK:
                    return StackBasedMazeSolver.class;
                case DFS:
                    return StackBasedDFSMazeSolver.class;
                case BFS:
                    return QueueBasedBFSMazeSolver.class;
            }
            throw new EnumConstantNotPresentException(Type.class, name());
        }

        public MazeSolver init() {
            switch (this) {
                case RECURSIVE:
                    return new RecursiveMazeSolver();
                case STACK:
                    return new StackBasedMazeSolver();
                case DFS:
                    return new StackBasedDFSMazeSolver();
                case BFS:
                    return new QueueBasedBFSMazeSolver();
            }
            throw new EnumConstantNotPresentException(Type.class, name());
        }

        public String description() {
            switch (this) {
                case RECURSIVE:
                    return "Recursive";
                case STACK:
                    return "Recursive - Stack Rewrite";
                case DFS:
                    return "DFS - Stack";
                case BFS:
                    return "BFS - Queue";
            }
            throw new EnumConstantNotPresentException(Type.class, name());
        }
    }

    public enum Direction {
        UP, RIGHT, DOWN, LEFT, NONE;

        public Loc reverse(int r, int c) {
            return new Loc(r - dx(), c - dy());
        }

        public Loc forward(Loc loc) {
            return forward(loc.getR(), loc.getC());
        }

        public Loc forward(int r, int c) {
            return new Loc(r + dx(), c + dy());
        }

        public int dx() {
            switch (this) {
                case DOWN:
                    return 1;
                case UP:
                    return -1;
            }
            return 0;
        }

        public int dy() {
            switch (this) {
                case RIGHT:
                    return 1;
                case LEFT:
                    return -1;
            }
            return 0;
        }
    }

    public interface MSEventListener<PathType> extends EventListener {
        void started(int r, int c, int tR, int tC, MazeCoder.Block[][] map);

        void tryout(int r, int c, MazeSolver.Direction direction, PathType path, MazeCoder.Block[][] map);

        void found(int tR, int tC, PathType path, MazeCoder.Block[][] map);

        void failed(int r, int c, PathType path, MazeCoder.Block[][] map);

        void ended(boolean hasPath, MazeCoder.Block[][] map);
    }

    public static class Loc {
        private int r, c;

        public Loc(int r, int c) {
            this.r = r;
            this.c = c;
        }

        public int getR() {
            return r;
        }

        public int getC() {
            return c;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Loc) {
                Loc loc = (Loc) obj;
                return r == loc.r && c == loc.c;
            }
            return false;
        }

        @Override
        public String toString() {
            return "(" + r + "," + c + ")";
        }
    }
}