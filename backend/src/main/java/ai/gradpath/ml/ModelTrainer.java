package ai.gradpath.ml;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Trains and evaluates the placement model - entirely in Java, deterministically for a given seed.
 *
 * <ol>
 *   <li>Stratified split: 80% training, 20% held-out test (never used for any choice).</li>
 *   <li>For each candidate L2 penalty, stratified 5-fold cross-validation on the training rows (the scaler is fitted
 *       inside each fold, so no information leaks from the validation fold).</li>
 *   <li>The penalty with the best mean ROC-AUC is chosen (the larger, simpler one wins near-ties).</li>
 *   <li>The decision threshold is the one that maximises F1 on the out-of-fold predictions.</li>
 *   <li>The final model is fitted on all training rows and evaluated once on the test rows.</li>
 * </ol>
 */
public final class ModelTrainer {

    public static final double[] LAMBDAS = {0.0, 0.1, 1.0, 10.0, 100.0, 1000.0};
    public static final int FOLDS = 5;
    public static final String VERSION = "logreg-1.0";
    /** Penalties whose CV ROC-AUC is within this of the best are treated as equally good. */
    static final double NEAR_TIE = 0.001;

    private ModelTrainer() { }

    public static PlacementModel train(PlacementDataset data, long seed) {
        if (data.size() < 50) {
            throw new IllegalArgumentException("At least 50 labelled rows are needed to train and test a model");
        }
        double rate = data.placementRate();
        if (rate == 0 || rate == 1) {
            throw new IllegalArgumentException("The dataset must contain both placed and not-placed students");
        }
        Random rng = new Random(seed);
        int[] fold = stratifiedAssignment(data.labels(), 5, rng);   // 5 parts: part 0 = test (20%)
        List<Integer> trainIdx = new ArrayList<>();
        List<Integer> testIdx = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            (fold[i] == 0 ? testIdx : trainIdx).add(i);
        }
        double[][] xTrain = rows(data, trainIdx);
        int[] yTrain = labels(data, trainIdx);
        double[][] xTest = rows(data, testIdx);
        int[] yTest = labels(data, testIdx);

        int[] cvFold = stratifiedAssignment(yTrain, FOLDS, rng);
        double bestAuc = -1;
        double bestLambda = LAMBDAS[0];
        double bestStd = 0;
        double[] bestOof = null;
        List<double[]> search = new ArrayList<>();
        List<double[]> oofs = new ArrayList<>();
        for (double lambda : LAMBDAS) {
            double[] oof = new double[yTrain.length];
            double[] aucs = new double[FOLDS];
            for (int k = 0; k < FOLDS; k++) {
                List<Integer> fit = new ArrayList<>();
                List<Integer> val = new ArrayList<>();
                for (int i = 0; i < yTrain.length; i++) {
                    (cvFold[i] == k ? val : fit).add(i);
                }
                double[][] xf = pick(xTrain, fit);
                int[] yf = pick(yTrain, fit);
                Scaler s = Scaler.fit(xf);
                LogisticRegression.Fit m = LogisticRegression.train(s.transform(xf), yf, lambda);
                double[] pv = new double[val.size()];
                int[] yv = new int[val.size()];
                for (int v = 0; v < val.size(); v++) {
                    int i = val.get(v);
                    pv[v] = m.probability(s.transform(xTrain[i]));
                    yv[v] = yTrain[i];
                    oof[i] = pv[v];
                }
                aucs[k] = ModelMetrics.rocAuc(pv, yv);
            }
            double mean = 0;
            for (double a : aucs) {
                mean += a / FOLDS;
            }
            double var = 0;
            for (double a : aucs) {
                var += (a - mean) * (a - mean) / FOLDS;
            }
            search.add(new double[] {lambda, ModelMetrics.r4(mean), ModelMetrics.r4(Math.sqrt(var))});
            oofs.add(oof);
        }
        double top = search.stream().mapToDouble(s -> s[1]).max().orElse(0);
        for (int c = 0; c < search.size(); c++) {          // lambdas ascend: the last near-best one is the simplest
            if (search.get(c)[1] >= top - NEAR_TIE) {
                bestAuc = search.get(c)[1];
                bestLambda = search.get(c)[0];
                bestStd = search.get(c)[2];
                bestOof = oofs.get(c);
            }
        }
        search.replaceAll(s -> new double[] {s[0], s[1]});
        double threshold = bestF1Threshold(bestOof, yTrain);

        Scaler scaler = Scaler.fit(xTrain);
        LogisticRegression.Fit fit = LogisticRegression.train(scaler.transform(xTrain), yTrain, bestLambda);
        double[] pTest = new double[yTest.length];
        for (int i = 0; i < yTest.length; i++) {
            pTest[i] = fit.probability(scaler.transform(xTest[i]));
        }
        ModelMetrics test = ModelMetrics.evaluate(pTest, yTest, threshold);

        double[] min = new double[PlacementFeatures.COUNT];
        double[] max = new double[PlacementFeatures.COUNT];
        java.util.Arrays.fill(min, Double.MAX_VALUE);
        java.util.Arrays.fill(max, -Double.MAX_VALUE);
        for (double[] r : data.rows()) {
            for (int j = 0; j < r.length; j++) {
                min[j] = Math.min(min[j], r[j]);
                max[j] = Math.max(max[j], r[j]);
            }
        }
        return new PlacementModel(PlacementModel.FORMAT, VERSION, Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                data.source(), data.synthetic(), data.size(), yTrain.length, yTest.length, ModelMetrics.r4(rate),
                PlacementFeatures.NAMES, round(scaler.mean), round(scaler.scale), min, max, round(fit.weights()),
                ModelMetrics.r4(fit.bias()), bestLambda, threshold, FOLDS, ModelMetrics.r4(bestAuc),
                ModelMetrics.r4(bestStd), search, test);
    }

    /** Threshold (0.05..0.95 in steps of 0.01) with the highest F1 on the given predictions. */
    static double bestF1Threshold(double[] p, int[] y) {
        double best = 0.5;
        double bestF1 = -1;
        for (int t = 5; t <= 95; t++) {
            double th = t / 100.0;
            int tp = 0;
            int fp = 0;
            int fn = 0;
            for (int i = 0; i < p.length; i++) {
                boolean pred = p[i] >= th;
                if (pred && y[i] == 1) {
                    tp++;
                } else if (pred) {
                    fp++;
                } else if (y[i] == 1) {
                    fn++;
                }
            }
            double f1 = tp == 0 ? 0 : 2.0 * tp / (2.0 * tp + fp + fn);
            if (f1 > bestF1 + 1e-12) {
                bestF1 = f1;
                best = th;
            }
        }
        return best;
    }

    /** Assign each row to one of k parts so every part has about the same share of each class. */
    static int[] stratifiedAssignment(int[] y, int k, Random rng) {
        int[] out = new int[y.length];
        for (int cls = 0; cls <= 1; cls++) {
            List<Integer> idx = new ArrayList<>();
            for (int i = 0; i < y.length; i++) {
                if (y[i] == cls) {
                    idx.add(i);
                }
            }
            java.util.Collections.shuffle(idx, rng);
            for (int j = 0; j < idx.size(); j++) {
                out[idx.get(j)] = j % k;
            }
        }
        return out;
    }

    private static double[][] rows(PlacementDataset d, List<Integer> idx) {
        return idx.stream().map(i -> d.rows().get(i)).toArray(double[][]::new);
    }

    private static int[] labels(PlacementDataset d, List<Integer> idx) {
        return idx.stream().mapToInt(i -> d.labels()[i]).toArray();
    }

    private static double[][] pick(double[][] x, List<Integer> idx) {
        return idx.stream().map(i -> x[i]).toArray(double[][]::new);
    }

    private static int[] pick(int[] y, List<Integer> idx) {
        return idx.stream().mapToInt(i -> y[i]).toArray();
    }

    private static double[] round(double[] v) {
        double[] out = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            out[i] = Math.round(v[i] * 1e8) / 1e8;
        }
        return out;
    }

    /** Standardisation (z-scores) fitted on training rows only. */
    record Scaler(double[] mean, double[] scale) {

        static Scaler fit(double[][] x) {
            int d = x[0].length;
            double[] mean = new double[d];
            double[] scale = new double[d];
            for (double[] r : x) {
                for (int j = 0; j < d; j++) {
                    mean[j] += r[j] / x.length;
                }
            }
            for (double[] r : x) {
                for (int j = 0; j < d; j++) {
                    scale[j] += (r[j] - mean[j]) * (r[j] - mean[j]) / x.length;
                }
            }
            for (int j = 0; j < d; j++) {
                scale[j] = Math.sqrt(scale[j]);
                if (scale[j] < 1e-12) {
                    scale[j] = 1;           // constant feature: leave it centred, avoid division by zero
                }
            }
            return new Scaler(mean, scale);
        }

        double[] transform(double[] r) {
            double[] z = new double[r.length];
            for (int j = 0; j < r.length; j++) {
                z[j] = (r[j] - mean[j]) / scale[j];
            }
            return z;
        }

        double[][] transform(double[][] x) {
            double[][] out = new double[x.length][];
            for (int i = 0; i < x.length; i++) {
                out[i] = transform(x[i]);
            }
            return out;
        }
    }
}
