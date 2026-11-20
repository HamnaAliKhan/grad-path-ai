package ai.gradpath.ml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A trained placement model as saved to (and loaded from) a JSON file: standardisation parameters, logistic
 * regression coefficients, the decision threshold and the evaluation results. It holds no student data.
 *
 * @param format         file format id ({@value #FORMAT})
 * @param version        model version shown in the UI
 * @param trainedAt      ISO-8601 time of training
 * @param dataSource     description of the training data
 * @param synthetic      true when trained on synthetic practice data
 * @param rows           usable rows in the dataset
 * @param trainRows      rows used for training and cross-validation
 * @param testRows       rows held out for the final test
 * @param placementRate  share of placed students in the dataset
 * @param features       feature keys (see {@link PlacementFeatures#NAMES})
 * @param mean           training mean of each feature
 * @param scale          training standard deviation of each feature
 * @param min            smallest training value of each feature (inputs are clamped to this range)
 * @param max            largest training value of each feature
 * @param weights        coefficients for the standardised features (log-odds per standard deviation)
 * @param bias           intercept
 * @param lambda         chosen L2 penalty
 * @param threshold      probability at or above which the model predicts "placed" (chosen to maximise F1)
 * @param cvFolds        number of cross-validation folds
 * @param cvAucMean      mean cross-validated ROC-AUC for the chosen penalty
 * @param cvAucStd       standard deviation of the fold ROC-AUCs
 * @param lambdaSearch   [lambda, mean CV ROC-AUC] for every penalty tried
 * @param test           metrics on the held-out test rows
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlacementModel(String format, String version, String trainedAt, String dataSource, boolean synthetic,
                             int rows, int trainRows, int testRows, double placementRate, List<String> features,
                             double[] mean, double[] scale, double[] min, double[] max, double[] weights, double bias,
                             double lambda, double threshold, int cvFolds, double cvAucMean, double cvAucStd,
                             List<double[]> lambdaSearch, ModelMetrics test) {

    public static final String FORMAT = "gradpath-logreg-v1";
    public static final String NAME = "Logistic regression (L2, Newton/IRLS)";

    /** One feature's effect on an individual estimate, in log-odds relative to an average training profile. */
    public record Contribution(String feature, String label, double value, double logOdds) { }

    /** Validate a loaded model file. */
    public PlacementModel {
        if (!FORMAT.equals(format)) {
            throw new IllegalArgumentException("Unsupported model format: " + format);
        }
        int d = PlacementFeatures.COUNT;
        if (features == null || !features.equals(PlacementFeatures.NAMES) || mean.length != d || scale.length != d
                || min.length != d || max.length != d || weights.length != d) {
            throw new IllegalArgumentException("The model file does not match the five expected features");
        }
    }

    /** Clamp to the training range, then standardise. */
    public double[] standardise(PlacementFeatures f) {
        double[] x = f.toArray();
        double[] z = new double[x.length];
        for (int j = 0; j < x.length; j++) {
            double v = Math.max(min[j], Math.min(max[j], x[j]));
            z[j] = (v - mean[j]) / scale[j];
        }
        return z;
    }

    /** True when any input lies outside the range seen in training (the estimate then uses the nearest edge). */
    public boolean outsideTrainingRange(PlacementFeatures f) {
        double[] x = f.toArray();
        for (int j = 0; j < x.length; j++) {
            if (x[j] < min[j] - 1e-9 || x[j] > max[j] + 1e-9) {
                return true;
            }
        }
        return false;
    }

    public double logit(PlacementFeatures f) {
        double[] z = standardise(f);
        double s = bias;
        for (int j = 0; j < z.length; j++) {
            s += weights[j] * z[j];
        }
        return s;
    }

    /** Estimated probability of placement, 0..1. */
    public double probability(PlacementFeatures f) {
        return LogisticRegression.sigmoid(logit(f));
    }

    /** Per-feature contributions, largest effect first. */
    public List<Contribution> contributions(PlacementFeatures f) {
        double[] x = f.toArray();
        double[] z = standardise(f);
        List<Contribution> out = new ArrayList<>();
        for (int j = 0; j < z.length; j++) {
            out.add(new Contribution(features.get(j), PlacementFeatures.LABELS.get(j), x[j], weights[j] * z[j]));
        }
        out.sort(Comparator.comparingDouble((Contribution c) -> Math.abs(c.logOdds())).reversed());
        return out;
    }

    /** Coefficients converted to odds ratios per one standard deviation, for the "About the Model" page. */
    public double oddsRatio(int feature) {
        return Math.exp(weights[feature]);
    }
}
