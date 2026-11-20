package ai.gradpath.ml;

/**
 * L2-regularised binary logistic regression trained with Newton's method (iteratively re-weighted least squares).
 *
 * <p>With only a handful of standardised features the Hessian is tiny, so each Newton step solves a small linear
 * system exactly and training converges in a few iterations - deterministically, with no random initialisation.
 * The intercept is not penalised.
 */
public final class LogisticRegression {

    private static final int MAX_ITERATIONS = 100;
    private static final double TOLERANCE = 1e-9;

    private LogisticRegression() { }

    /** Fitted coefficients: {@code weights} for the (already scaled) features and the {@code bias}. */
    public record Fit(double[] weights, double bias, int iterations) {

        public double logit(double[] z) {
            double s = bias;
            for (int j = 0; j < weights.length; j++) {
                s += weights[j] * z[j];
            }
            return s;
        }

        public double probability(double[] z) {
            return sigmoid(logit(z));
        }
    }

    public static double sigmoid(double t) {
        if (t >= 0) {
            return 1 / (1 + Math.exp(-t));
        }
        double e = Math.exp(t);
        return e / (1 + e);
    }

    /**
     * Fit the model.
     *
     * @param x      scaled feature rows
     * @param y      0/1 labels
     * @param lambda L2 penalty strength (larger = simpler model); 0 disables it
     */
    public static Fit train(double[][] x, int[] y, double lambda) {
        int n = x.length;
        if (n == 0 || n != y.length) {
            throw new IllegalArgumentException("Training data is empty or mismatched");
        }
        int d = x[0].length;
        int p = d + 1;                          // index 0 = intercept
        double[] beta = new double[p];
        int it = 0;
        for (; it < MAX_ITERATIONS; it++) {
            double[] grad = new double[p];
            double[][] hess = new double[p][p];
            for (int i = 0; i < n; i++) {
                double eta = beta[0];
                for (int j = 0; j < d; j++) {
                    eta += beta[j + 1] * x[i][j];
                }
                double mu = sigmoid(eta);
                double w = Math.max(mu * (1 - mu), 1e-10);
                double r = y[i] - mu;
                grad[0] += r;
                hess[0][0] += w;
                for (int j = 0; j < d; j++) {
                    double xj = x[i][j];
                    grad[j + 1] += r * xj;
                    hess[0][j + 1] += w * xj;
                    for (int k = j; k < d; k++) {
                        hess[j + 1][k + 1] += w * xj * x[i][k];
                    }
                }
            }
            for (int j = 0; j < p; j++) {
                for (int k = 0; k < j; k++) {
                    hess[j][k] = hess[k][j];
                }
            }
            for (int j = 1; j < p; j++) {
                grad[j] -= lambda * beta[j];
                hess[j][j] += lambda;
            }
            double[] step = solve(hess, grad);
            double change = 0;
            for (int j = 0; j < p; j++) {
                beta[j] += step[j];
                change = Math.max(change, Math.abs(step[j]));
            }
            if (change < TOLERANCE) {
                it++;
                break;
            }
        }
        double[] weights = new double[d];
        System.arraycopy(beta, 1, weights, 0, d);
        return new Fit(weights, beta[0], it);
    }

    /** Solve A x = b by Gaussian elimination with partial pivoting (A is small and positive definite). */
    static double[] solve(double[][] a, double[] b) {
        int n = b.length;
        double[][] m = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(a[i], 0, m[i], 0, n);
            m[i][n] = b[i];
        }
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int r = col + 1; r < n; r++) {
                if (Math.abs(m[r][col]) > Math.abs(m[pivot][col])) {
                    pivot = r;
                }
            }
            double[] tmp = m[col];
            m[col] = m[pivot];
            m[pivot] = tmp;
            double diag = m[col][col];
            if (Math.abs(diag) < 1e-12) {
                throw new ArithmeticException("Singular system - a feature may be constant in the training data");
            }
            for (int r = col + 1; r < n; r++) {
                double f = m[r][col] / diag;
                for (int c = col; c <= n; c++) {
                    m[r][c] -= f * m[col][c];
                }
            }
        }
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double s = m[i][n];
            for (int j = i + 1; j < n; j++) {
                s -= m[i][j] * x[j];
            }
            x[i] = s / m[i][i];
        }
        return x;
    }
}
