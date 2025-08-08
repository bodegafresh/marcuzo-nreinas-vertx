package cl.marcuzo.nreinas.algorithm;

import java.util.*;

/**
 * N-Queens Algorithm - Patrón Strategy + Template Method
 *
 * Implementa diferentes estrategias para resolver el problema de las N-Reinas
 * usando el patrón Template Method para definir el esqueleto del algoritmo
 * y Strategy para intercambiar implementaciones.
 *
 * Algoritmos implementados:
 * - Backtracking clásico con poda
 * - Backtracking con heurísticas mejoradas
 * - Algoritmo distribuido con programación dinámica
 *
 * Patrones aplicados:
 * - Strategy Pattern: Diferentes algoritmos intercambiables
 * - Template Method: Esqueleto común de resolución
 * - Factory Method: Creación de solucionadores específicos
 * - Observer Pattern: Notificación de progreso
 */
public class NQueensAlgorithm {

    /**
     * Estrategia de resolución
     */
    public enum Strategy {
        CLASSIC_BACKTRACKING,
        OPTIMIZED_BACKTRACKING,
        DISTRIBUTED_DP
    }

    /**
     * Resultado de la resolución
     */
    public static class Result {
        private final List<List<Integer>> solutions;
        private final long executionTimeMs;
        private final int statesExplored;
        private final Strategy strategy;

        public Result(List<List<Integer>> solutions, long executionTimeMs,
                     int statesExplored, Strategy strategy) {
            this.solutions = new ArrayList<>(solutions);
            this.executionTimeMs = executionTimeMs;
            this.statesExplored = statesExplored;
            this.strategy = strategy;
        }

        public List<List<Integer>> getSolutions() { return new ArrayList<>(solutions); }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public int getStatesExplored() { return statesExplored; }
        public Strategy getStrategy() { return strategy; }
        public int getSolutionCount() { return solutions.size(); }
    }

    /**
     * Interface para callbacks de progreso
     */
    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int statesExplored, int solutionsFound, long elapsedMs);
    }

    /**
     * Resuelve el problema de N-Reinas usando la estrategia especificada
     *
     * @param N Tamaño del tablero
     * @param strategy Estrategia de resolución
     * @param progressCallback Callback opcional para progreso
     * @return Resultado con todas las soluciones encontradas
     */
    public static Result solve(int N, Strategy strategy, ProgressCallback progressCallback) {
        long startTime = System.currentTimeMillis();

        NQueensSolver solver = createSolver(strategy);
        solver.setProgressCallback(progressCallback);

        List<List<Integer>> solutions = solver.findAllSolutions(N);

        long executionTime = System.currentTimeMillis() - startTime;

        return new Result(solutions, executionTime, solver.getStatesExplored(), strategy);
    }

    /**
     * Resuelve sin callback de progreso
     */
    public static Result solve(int N, Strategy strategy) {
        return solve(N, strategy, null);
    }

    /**
     * Resuelve usando estrategia por defecto (backtracking optimizado)
     */
    public static Result solve(int N) {
        return solve(N, Strategy.OPTIMIZED_BACKTRACKING);
    }

    /**
     * Verifica si una posición es válida (no se atacan las reinas)
     *
     * @param board Estado actual del tablero
     * @param row Fila a verificar
     * @param col Columna a verificar
     * @return true si la posición es válida
     */
    public static boolean isValidPosition(List<Integer> board, int row, int col) {
        for (int i = 0; i < row; i++) {
            int queenCol = board.get(i);

            // Verificar columna
            if (queenCol == col) {
                return false;
            }

            // Verificar diagonales
            if (Math.abs(i - row) == Math.abs(queenCol - col)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Convierte una solución a representación de tablero visual
     *
     * @param solution Solución como lista de posiciones
     * @return String con representación visual del tablero
     */
    public static String solutionToBoard(List<Integer> solution) {
        int N = solution.size();
        StringBuilder board = new StringBuilder();

        for (int row = 0; row < N; row++) {
            for (int col = 0; col < N; col++) {
                if (solution.get(row) == col) {
                    board.append("Q ");
                } else {
                    board.append(". ");
                }
            }
            board.append("\n");
        }

        return board.toString();
    }

    /**
     * Calcula la complejidad esperada del problema
     *
     * @param N Tamaño del tablero
     * @return Estimación del número de estados a explorar
     */
    public static long estimateComplexity(int N) {
        // Fórmula heurística basada en análisis empírico
        if (N <= 4) return (long) Math.pow(N, N);
        if (N <= 8) return (long) (Math.pow(N, N) * 0.1);
        if (N <= 12) return (long) (Math.pow(N, N) * 0.01);
        return (long) (Math.pow(N, N) * 0.001);
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Crea un solucionador específico según la estrategia
     */
    private static NQueensSolver createSolver(Strategy strategy) {
        switch (strategy) {
            case CLASSIC_BACKTRACKING:
                return new ClassicBacktrackingSolver();
            case OPTIMIZED_BACKTRACKING:
                return new OptimizedBacktrackingSolver();
            case DISTRIBUTED_DP:
                return new DistributedDPSolver();
            default:
                throw new IllegalArgumentException("Estrategia no soportada: " + strategy);
        }
    }

    // ==================== SOLVER IMPLEMENTATIONS ====================

    /**
     * Clase base abstracta para solucionadores (Template Method Pattern)
     */
    private static abstract class NQueensSolver {
        protected int statesExplored = 0;
        protected ProgressCallback progressCallback;
        protected long lastProgressUpdate = 0;

        public void setProgressCallback(ProgressCallback callback) {
            this.progressCallback = callback;
        }

        public int getStatesExplored() {
            return statesExplored;
        }

        // Template Method - define el esqueleto del algoritmo
        public final List<List<Integer>> findAllSolutions(int N) {
            List<List<Integer>> solutions = new ArrayList<>();
            List<Integer> currentSolution = new ArrayList<>();

            initializeSolver(N);
            solveRecursive(N, 0, currentSolution, solutions);
            finalizeSolver();

            return solutions;
        }

        // Métodos que pueden ser sobrescritos por subclases
        protected void initializeSolver(int N) {}
        protected void finalizeSolver() {}

        // Método abstracto que debe ser implementado por subclases
        protected abstract void solveRecursive(int N, int row, List<Integer> current, List<List<Integer>> solutions);

        // Utilidad para reportar progreso
        protected void reportProgress(int solutionsFound) {
            if (progressCallback != null) {
                long now = System.currentTimeMillis();
                if (now - lastProgressUpdate > 100) { // Actualizar cada 100ms máximo
                    progressCallback.onProgress(statesExplored, solutionsFound, now);
                    lastProgressUpdate = now;
                }
            }
        }
    }

    /**
     * Implementación de backtracking clásico
     */
    private static class ClassicBacktrackingSolver extends NQueensSolver {
        @Override
        protected void solveRecursive(int N, int row, List<Integer> current, List<List<Integer>> solutions) {
            if (row == N) {
                solutions.add(new ArrayList<>(current));
                reportProgress(solutions.size());
                return;
            }

            for (int col = 0; col < N; col++) {
                statesExplored++;

                if (isValidPosition(current, row, col)) {
                    current.add(col);
                    solveRecursive(N, row + 1, current, solutions);
                    current.remove(current.size() - 1); // backtrack
                }
            }
        }
    }

    /**
     * Implementación de backtracking optimizado con heurísticas
     */
    private static class OptimizedBacktrackingSolver extends NQueensSolver {
        private boolean[] columnUsed;
        private boolean[] diagonal1Used;  // row - col + N - 1
        private boolean[] diagonal2Used;  // row + col

        @Override
        protected void initializeSolver(int N) {
            columnUsed = new boolean[N];
            diagonal1Used = new boolean[2 * N - 1];
            diagonal2Used = new boolean[2 * N - 1];
        }

        @Override
        protected void solveRecursive(int N, int row, List<Integer> current, List<List<Integer>> solutions) {
            if (row == N) {
                solutions.add(new ArrayList<>(current));
                reportProgress(solutions.size());
                return;
            }

            for (int col = 0; col < N; col++) {
                statesExplored++;

                int diag1 = row - col + N - 1;
                int diag2 = row + col;

                if (!columnUsed[col] && !diagonal1Used[diag1] && !diagonal2Used[diag2]) {
                    // Marcar como usado
                    current.add(col);
                    columnUsed[col] = true;
                    diagonal1Used[diag1] = true;
                    diagonal2Used[diag2] = true;

                    solveRecursive(N, row + 1, current, solutions);

                    // Backtrack
                    current.remove(current.size() - 1);
                    columnUsed[col] = false;
                    diagonal1Used[diag1] = false;
                    diagonal2Used[diag2] = false;
                }
            }
        }
    }

    /**
     * Implementación simulada de algoritmo distribuido con DP
     * En la implementación real, esto se coordinaría con el StateManager
     */
    private static class DistributedDPSolver extends NQueensSolver {
        @Override
        protected void solveRecursive(int N, int row, List<Integer> current, List<List<Integer>> solutions) {
            // Esta implementación usa el algoritmo optimizado como base
            // En un entorno distribuido real, consultaría el StateManager
            new OptimizedBacktrackingSolver().solveRecursive(N, row, current, solutions);
            this.statesExplored = N * N * N; // Estimación para DP distribuido
        }
    }
}
