package io.github.apollozhu.controller;

import io.github.apollozhu.model.MazeCoder;
import io.github.apollozhu.model.solver.MazeSolver;
import io.github.apollozhu.view.MazeCanvas;
import io.github.apollozhu.view.SpringUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

/**
 * @author ApolloZhu, Pd. 1
 */
public class MazePanel extends PlaybackPanel implements MazeSolver.MSEventListener {
    private static final MazeCoder.Block[][] LAU_MAZE = MazeCoder.decodeLauMaze();
    private static MazeSolver.Type[] types = MazeSolver.Type.values();
    private final JButton pickStartButton, pickEndButton, editWallButton;
    private final JComboBox<String> solverComboBox;
    private final JTextField rowTextField, columnTextField, percentageTextField;
    private final JPanel panel = new JPanel(),
            mapGenerationControlPanel = new JPanel(), controlsPanel = new JPanel();
    private MazeSolver solver;
    private MazeCanvas canvas;
    private boolean isSelectingStart, isSelectingEnd, isEditingWall;
    private MazeSolver.Loc start, end;
    private MazeCoder.Block[][] map;
    private int selectedSolverIndex;
    private double pathPercentage = 0.7;


    public MazePanel() {
        add(panel, BorderLayout.NORTH);
        panel.setLayout(new GridLayout(2, 1));
        panel.add(mapGenerationControlPanel);
        mapGenerationControlPanel.setLayout(new SpringLayout());

        mapGenerationControlPanel.add(new JLabel("Row: "));
        mapGenerationControlPanel.add(rowTextField = new JTextField("" + LAU_MAZE.length));
        rowTextField.addActionListener(this::regenerateMap);
        mapGenerationControlPanel.add(new JLabel("Column: "));
        mapGenerationControlPanel.add(
                columnTextField = new JTextField("" + LAU_MAZE[0].length));
        columnTextField.addActionListener(this::regenerateMap);
        mapGenerationControlPanel.add(new JLabel("Path percentage: "));
        mapGenerationControlPanel.add(percentageTextField = new JTextField("" + pathPercentage));
        percentageTextField.addActionListener(this::regenerateMap);
        JButton regenerateButton = new JButton("Re-Generate");
        mapGenerationControlPanel.add(regenerateButton);
        regenerateButton.addActionListener(this::regenerateMap);
        SpringUtilities.makeCompactGrid(mapGenerationControlPanel,
                1, mapGenerationControlPanel.getComponentCount(),
                8, 8, 8, 8);

        panel.add(controlsPanel);
        controlsPanel.setLayout(new SpringLayout());
        controlsPanel.add(pickStartButton = new JButton("Pick Start"));
        pickStartButton.addActionListener(l -> {
            isSelectingEnd = false;
            isEditingWall = false;
            isSelectingStart = true;
        });
        controlsPanel.add(pickEndButton = new JButton("Pick End"));
        pickEndButton.addActionListener(l -> {
            isSelectingStart = false;
            isEditingWall = false;
            isSelectingEnd = true;
        });
        controlsPanel.add(editWallButton = new JButton("Edit Wall"));
        editWallButton.addActionListener(l -> {
            isSelectingStart = false;
            isSelectingEnd = false;
            isEditingWall = true;
        });

        controlsPanel.add(new JLabel("Solver: ", SwingConstants.TRAILING));
        String[] solverTypes = Arrays.stream(types)
                .map(MazeSolver.Type::description).toArray(String[]::new);
        controlsPanel.add(solverComboBox = new JComboBox<>(solverTypes));
        setMazeSolverAtIndex(selectedSolverIndex);
        solverComboBox.addActionListener(ignored ->
                setMazeSolverAtIndex(solverComboBox.getSelectedIndex()));

        SpringUtilities.makeGrid(controlsPanel,
                1, controlsPanel.getComponentCount(),
                8, 8, 8, 8);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                MazeCanvas canvas = (MazeCanvas) getCenterComponent();
                int x = e.getX() - canvas.getX();
                int y = e.getY() - canvas.getY();
                MazeSolver.Loc loc = canvas.getLoc(x, y);
                if (loc == null || loc.getR() < 0 || loc.getC() < 0
                        || loc.getR() >= map.length || loc.getC() >= map[loc.getR()].length) return;
                boolean notWall = !canvas.isWall(loc.getR(), loc.getC());
                if (isSelectingStart && notWall) {
                    canvas.setStart(start = loc);
                    isSelectingStart = false;
                } else if (isSelectingEnd && notWall) {
                    canvas.setTarget(end = loc);
                    isSelectingEnd = false;
                } else if (isEditingWall && !loc.equals(start) && !loc.equals(end)) {
                    MazeCoder.clear(map);
                    canvas.setMap(map);
                    map[loc.getR()][loc.getC()] = notWall ? MazeCoder.Block.WALL : MazeCoder.Block.EMPTY;
                    canvas.setMap(map);
                } else return;
                clearMap();
            }
        });
        resetStartEnd();
    }

    @Override
    protected Component getCenterComponent() {
        if (canvas != null) return canvas;
        if (map != null) return canvas = new MazeCanvas(map);
        return canvas = new MazeCanvas(map = LAU_MAZE);
    }

    protected void setMazeSolverAtIndex(int index) {
        index = Math.max(Math.min(types.length, index), 0);
        if (solver != null && selectedSolverIndex == index) return;
        setMazeSolver(types[selectedSolverIndex = index].init());
    }

    protected void setMazeSolver(MazeSolver solver) {
        terminate();
        clearMap();
        if (this.solver != null)
            this.solver.removeEventListener(canvas);
        this.solver = solver;
        for (int i = 0; i < types.length; i++)
            if (types[i].getClass().equals(solver.getClass())) {
                solverComboBox.setSelectedIndex(i);
                break;
            }
        solver.addEventListener(canvas);
    }

    protected void regenerateMap(ActionEvent ignored) {
        int r = map.length, c = map[0].length, newR = r, newC = c;
        double newPrecentage = pathPercentage;
        try {
            newR = Integer.parseUnsignedInt(rowTextField.getText());
        } catch (Exception e) {
            rowTextField.setText("" + r);
        }
        try {
            newC = Integer.parseUnsignedInt(columnTextField.getText());
        } catch (Exception e) {
            columnTextField.setText("" + c);
        }
        try {
            newPrecentage = Double.parseDouble(percentageTextField.getText());
        } catch (Exception e) {
            percentageTextField.setText("" + pathPercentage);
        }
        setMap(MazeCoder.generate(newR, newC, pathPercentage = newPrecentage));
    }

    @Override
    protected void start() {
        for (Component comp : mapGenerationControlPanel.getComponents()) comp.setEnabled(false);
        for (Component comp : controlsPanel.getComponents()) comp.setEnabled(false);
        isEditingWall = false;
        clearMap();
        solver.addEventListener(this);
        solver.start(map, start.getR(), start.getC(), end.getR(), end.getC());
    }

    @Override
    protected void terminate() {
        terminate(false);
    }

    protected void terminate(boolean hasPath) {
        for (Component comp : mapGenerationControlPanel.getComponents()) comp.setEnabled(true);
        for (Component comp : controlsPanel.getComponents()) comp.setEnabled(true);
        if (solver != null) {
            solver.removeEventListener(this);
            solver.stop(hasPath);
        }
        super.terminate();
    }

    protected void setMap(MazeCoder.Block[][] newMap) {
        map = newMap.clone();
        canvas.resetMap(map);
        resetStartEnd();
    }

    protected void resetStartEnd() {
        map[0][0] = map[map.length - 1][map[0].length - 1] = MazeCoder.Block.EMPTY;
        canvas.setStart(start = new MazeSolver.Loc(0, 0));
        canvas.setTarget(end = new MazeSolver.Loc(map.length - 1, map[0].length - 1));
    }

    protected void clearMap() {
        MazeCoder.clear(map);
        canvas.resetMap(map);
    }

    @Override
    public void started(int r, int c, int tR, int tC, MazeCoder.Block[][] map) {
        sleep();
    }

    @Override
    public void tryout(int r, int c, MazeSolver.Direction direction, Object path, MazeCoder.Block[][] map) {
        sleep();
    }

    @Override
    public void found(int tR, int tC, Object path, MazeCoder.Block[][] map) {
        sleep();
    }

    @Override
    public void failed(int r, int c, Object path, MazeCoder.Block[][] map) {
        sleep();
    }

    @Override
    public void ended(boolean hasPath, MazeCoder.Block[][] map) {
        JOptionPane.showMessageDialog(this, hasPath ? "It is doable." : "Can do better.");
        terminate(hasPath);
    }
}