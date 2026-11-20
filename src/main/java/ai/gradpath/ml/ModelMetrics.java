package ai.gradpath.ml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Classification quality of a probability model on held-out data.
 *
 * @param rows        number of evaluated rows
 * @param accuracy    share of correct yes/no predictions at the decision threshold
 * @param precision   of the students predicted "placed", the share who were
 * @param recall      of the students who were placed, the share the model found
 * @param f1          harmonic mean of precision and recall
 * @param rocAuc      probability that a random placed student scores above a random non-placed one (0.5 = chance)
 * @param brier       mean squared error of the probabilities (lower is better)
 * @param logLoss     mean negative log-likelihood (lower is better)
 * @param tp          true positives
 * @param fp          false positives
 * @param tn          true negatives
 * @param fn          false negatives
 * @param roc         ROC curve points [false-positive rate, true-positive rate], thinned for charting
 * @param calibration reliability points [mean predicted, observed rate, count] for ten probability bins
 */
public record ModelMetrics(int rows, double accuracy, double precision, double recall, double f1, double rocAuc,
                           double brier, double logLoss, int tp, int fp, int tn, int fn,
                           List<double[]> roc, List<double[]> calibration) {

    public static ModelMetrics evaluate(double[] probabilities, int[] labels, double threshold) {
        int n = labels.length;
        int tp = 0;
        int fp = 0;
        int tn = 0;
        int fn = 0;
        double brier = 0;
        double logLoss = 0;
        for (int i = 0; i < n; i++) {
            double p = probabilities[i];
            boolean predicted = p >= threshold;
            if (predicted && labels[i] == 1) {
                tp++;
            } else if (predicted) {
                fp++;
            } else if (labels[i] == 1) {
                fn++;
            } else {
                tn++;
            }
            brier += (p - labels[i]) * (p - labels[i]);
            double c = Math.min(1 - 1e-15, Math.max(1e-15, p));
            logLoss -= labels[i] == 1 ? Math.log(c) : Math.log(1 - c);
        }
        double precision = tp + fp == 0 ? 0 : (double) tp / (tp + fp);
        double recall = tp + fn == 0 ? 0 : (double) tp / (tp + fn);
        double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
        return new ModelMetrics(n, r4((double) (tp + tn) / n), r4(precision), r4(recall), r4(f1),
                r4(rocAuc(probabilities, labels)), r4(brier / n), r4(logLoss / n), tp, fp, tn, fn,
                rocCurve(probabilities, labels, 60), calibration(probabilities, labels, 10));
    }

    /** ROC-AUC via the rank-sum (Mann-Whitney U) formula; ties receive their average rank. */
    public static double rocAuc(double[] p, int[] y) {
        int n = p.length;
        Integer[] idx = IntStream.range(0, n).boxed().toArray(Integer[]::new);
        Arrays.sort(idx, Comparator.comparingDouble(i -> p[i]));
        double[] rank = new double[n];
        int i = 0;
        while (i < n) {
            int j = i;
            while (j + 1 < n && p[idx[j + 1]] == p[idx[i]]) {
                j++;
            }
            double avg = (i + j) / 2.0 + 1;
            for (int k = i; k <= j; k++) {
                rank[idx[k]] = avg;
            }
            i = j + 1;
        }
        long pos = Arrays.stream(y).filter(v -> v == 1).count();
        long neg = n - pos;
        if (pos == 0 || neg == 0) {
            return 0.5;
        }
        double sum = 0;
        for (int k = 0; k < n; k++) {
            if (y[k] == 1) {
                sum += rank[k];
            }
        }
        return (sum - pos * (pos + 1) / 2.0) / ((double) pos * neg);
    }

    /** ROC points from the highest threshold down, keeping at most {@code maxPoints} plus the end points. */
    static List<double[]> rocCurve(double[] p, int[] y, int maxPoints) {
        int n = p.length;
        Integer[] idx = IntStream.range(0, n).boxed().toArray(Integer[]::new);
        Arrays.sort(idx, Comparator.comparingDouble((Integer i) -> p[i]).reversed());
        long pos = Arrays.stream(y).filter(v -> v == 1).count();
        long neg = n - pos;
        List<double[]> all = new ArrayList<>();
        all.add(new double[] {0, 0});
        int tp = 0;
        int fp = 0;
        for (int k = 0; k < n; k++) {
            if (y[idx[k]] == 1) {
                tp++;
            } else {
                fp++;
            }
            if (k == n - 1 || p[idx[k + 1]] != p[idx[k]]) {
                all.add(new double[] {neg == 0 ? 0 : r4((double) fp / neg), pos == 0 ? 0 : r4((double) tp / pos)});
            }
        }
        if (all.size() <= maxPoints) {
            return all;
        }
        List<double[]> thin = new ArrayList<>();
        double step = (all.size() - 1) / (double) (maxPoints - 1);
        for (int k = 0; k < maxPoints; k++) {
            thin.add(all.get((int) Math.round(k * step)));
        }
        return thin;
    }

    /** Ten equal-width probability bins: mean predicted probability vs observed placement rate. */
    static List<double[]> calibration(double[] p, int[] y, int bins) {
        double[] sumP = new double[bins];
        double[] sumY = new double[bins];
        int[] count = new int[bins];
        for (int i = 0; i < p.length; i++) {
            int b = Math.min(bins - 1, (int) (p[i] * bins));
            sumP[b] += p[i];
            sumY[b] += y[i];
            count[b]++;
        }
        List<double[]> out = new ArrayList<>();
        for (int b = 0; b < bins; b++) {
            if (count[b] > 0) {
                out.add(new double[] {r4(sumP[b] / count[b]), r4(sumY[b] / count[b]), count[b]});
            }
        }
        return out;
    }

    static double r4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
