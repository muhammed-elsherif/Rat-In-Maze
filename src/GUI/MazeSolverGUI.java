/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package osproject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import model.MazeSolverOneThreadRecursion;

/**
 *
 * @author PC
 */
public class MazeSolverGUI extends javax.swing.JFrame {

    public MazeSolverGUI() {
        initComponents();
    }

    private static final int CELL_SIZE = 50; // Size of each cell in pixels

    private int threadId;
    private JButton submitButton;
    private JPanel mazePanel;
    private int mazeSize;
    private int[][] mazeMatrix;
    private boolean[][] visitedCells;

    private MazeSolverOneThreadRecursion mazeSolverThread;

    private static final Color[] THREAD_COLORS = {Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN};
    private List<MazeSolverThread> solverThreads;
    private ReentrantLock lock;

    public MazeSolverGUI(int mazeSize) {
        this.mazeSize = mazeSize;
        mazeMatrix = new int[mazeSize][mazeSize];
        visitedCells = new boolean[mazeSize][mazeSize];

        // Initialize maze matrix with all cells set to 1 (accessible || free)
        for (int i = 0; i < mazeSize; i++) {
            for (int j = 0; j < mazeSize; j++) {
                mazeMatrix[i][j] = 1;
                visitedCells[i][j] = false;
            }
        }
        initializeGUI();
    }

    private void initializeGUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Rat-In-Maze Solver");
//        setResizable(false);

        mazePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawMaze(g);
            }
        };
        mazePanel.setPreferredSize(new Dimension(mazeSize * CELL_SIZE, mazeSize * CELL_SIZE));
        mazePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = e.getY() / CELL_SIZE;
                int col = e.getX() / CELL_SIZE;
                toggleCell(row, col);
                mazePanel.repaint();
            }
        });

        submitButton = new JButton("Submit");
        submitButton.addActionListener((ActionEvent e) -> {
            startMazeSolver();
        });
        getContentPane().add(submitButton, BorderLayout.SOUTH);

        getContentPane().add(mazePanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        solverThreads = new ArrayList<>();
        lock = new ReentrantLock();

        startMazeSolver();
    }

    private void drawMaze(Graphics g) {
        for (int i = 0; i < mazeSize; i++) {
            for (int j = 0; j < mazeSize; j++) {
                int cellValue = mazeMatrix[i][j];
                if (cellValue == 0) {
                    g.setColor(Color.BLACK);
                } else {
                    if (visitedCells[i][j]) {
                        g.setColor(MazeSolverGUI.THREAD_COLORS[threadId % MazeSolverGUI.THREAD_COLORS.length]);
                        System.out.println("Thread Color: " + threadId);
                    } else {
                        g.setColor(Color.WHITE);
                    }
                }
                g.fillRect(j * CELL_SIZE, i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                g.setColor(Color.BLACK);
                g.drawRect(j * CELL_SIZE, i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
    }

    private void toggleCell(int row, int col) {
        if (row >= 0 && row < mazeSize && col >= 0 && col < mazeSize) {
            mazeMatrix[row][col] = 1 - mazeMatrix[row][col]; // Toggle cell value (0 to 1 or 1 to 0)
        }
    }

    private void startMazeSolver() {
        // Check if any cell has a value of 0
        boolean hasObstacle = false;
        for (int i = 0; i < mazeSize; i++) {
            for (int j = 0; j < mazeSize; j++) {
                if (mazeMatrix[i][j] == 0) {
                    hasObstacle = true;
                    break;
                }
            }
            if (hasObstacle) {
                break;
            }
        }

        if (!hasObstacle) {
            JOptionPane.showMessageDialog(this, "Please set at least one obstacle (cell with value 0).");
            return;
        }
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COLORS.length); // Limiting the number of threads to 6
        for (int i = 0; i < THREAD_COLORS.length; i++) {
            MazeSolverThread solverThread = new MazeSolverThread(i);
            solverThreads.add(solverThread);
            executorService.submit(solverThread);
        }
        executorService.shutdown();
//        mazeSolverThread = new MazeSolverOneThreadRecursion(mazeMatrix, visitedCells, this);
//        Thread solverThread = new Thread(mazeSolverThread);
//        solverThread.start();
    }

    private class MazeSolverThread implements Runnable {

        private int id;
        private int currentRow;
        private int currentCol;
        private Color color;

        public MazeSolverThread(int id) {
            this.id = id;
            this.currentRow = 0;
            this.currentCol = 0;
            this.color = THREAD_COLORS[id % THREAD_COLORS.length];
        }

        @Override
        public void run() {
            solveMaze(currentRow, currentCol);
        }

        private void solveMaze(int row, int col) {
            // Acquire the lock to ensure mutual exclusion when updating the GUI          
            lock.lock();
            Color threadColor = color;
            try {
                visitedCells[row][col] = true;
                mazePanel.repaint(); // Update the GUI
                threadId = id;
            } finally {
                // Release the lock
                lock.unlock();
            }

            try {
                Thread.sleep(1000); // Sleep for 500 milliseconds between each step
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("Thread " + id + " - Color: " + threadColor);

            if (row == mazeSize - 1 && col == mazeSize - 1) {
                JOptionPane.showMessageDialog(MazeSolverGUI.this, "Maze solved by Thread " + id + "!");
                return;
            }

            // Move right if it's possible
            if (col < mazeSize - 1 && mazeMatrix[row][col + 1] == 1 && !visitedCells[row][col + 1]) {
                solveMaze(row, col + 1);
            }

            // Move down if it's possible
            if (row < mazeSize - 1 && mazeMatrix[row + 1][col] == 1 && !visitedCells[row + 1][col]) {
                solveMaze(row + 1, col);
            }
        }
    }

    // Method called by MazeSolverOneThreadRecursion to update GUI with rat's movement
    public synchronized void updateRatPosition(int row, int col) {
        visitedCells[row][col] = true;
        mazePanel.repaint();
    }

    public synchronized void updateRatPositionFalse(int row, int col) {
        visitedCells[row][col] = false;
        mazePanel.repaint();
    }

    // Method called by MazeSolverOneThreadRecursion to set successful path in GUI
    public synchronized void setSuccessfulPath(List<int[]> successfulPath) {
        for (int[] cell : successfulPath) {
            visitedCells[cell[0]][cell[1]] = true;
        }
        mazePanel.repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MazeSolverGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        System.out.print("Enter the dimensions of the maze (N x N): ");
        Scanner input = new Scanner(System.in);
        int mazeSize = input.nextInt();
        SwingUtilities.invokeLater(() -> new MazeSolverGUI(mazeSize));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
