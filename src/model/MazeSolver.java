package model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import osproject.MazeSolverGUI;
import java.util.concurrent.Executors;

public class MazeSolver implements Runnable {

    private int[][] mazeMatrix;
    private boolean[][] visitedCells;
    private List<int[]> successfullPath;
    private MazeSolverGUI mazeSolverGUI;

    public MazeSolver(int[][] mazeMatrix, boolean[][] visitedCells, MazeSolverGUI mazeSolverGUI) {
        this.mazeMatrix = mazeMatrix;
        this.visitedCells = visitedCells;
        this.mazeSolverGUI = mazeSolverGUI;
        successfullPath = new ArrayList<>();
    }

    @Override
    public void run() {
        ExecutorService executorService = Executors.newFixedThreadPool(6); // Limiting the number of threads to 6
        solveMaze(0, 0);
        executorService.shutdown();
    }

    private boolean solveMaze(int row, int col) {
        if (!isPathValid(row, col)) {
            return false;
        }

        if (row == mazeMatrix.length - 1 && col == mazeMatrix[0].length - 1) {
            // Destination reached, mark current cell as part of the successful path
            successfullPath.add(new int[]{row, col});
            mazeSolverGUI.setSuccessfulPath(successfullPath);
            return true;
        }

        markCellAsVisited(row, col);

        // Update GUI to show the rat's movement
        mazeSolverGUI.updateRatPosition(row, col);

        try {
            Thread.sleep(100); // Delay to visualize the movement
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//         Move down
        if (solveMaze(row + 1, col)) {
            successfullPath.add(new int[]{row, col});
            return true;
        }

        // Move right
        if (solveMaze(row, col + 1)) {
            successfullPath.add(new int[]{row, col});
            return true;
        }
//        executorService.execute(() -> solveMaze(row + 1, col, executorService));
//
//        executorService.execute(() -> solveMaze(row, col + 1, executorService));

        // Backtrack
        markCellAsUnvisited(row, col);
        mazeSolverGUI.updateRatPositionFalse(row, col);

        return false;
    }

    private boolean isPathValid(int row, int col) {
        return row >= 0 && row < mazeMatrix.length
                && col >= 0 && col < mazeMatrix[0].length
                && mazeMatrix[row][col] == 1
                && !visitedCells[row][col];
    }

    private void markCellAsVisited(int row, int col) {
        visitedCells[row][col] = true;
    }

    private void markCellAsUnvisited(int row, int col) {
        visitedCells[row][col] = false;
    }
}
