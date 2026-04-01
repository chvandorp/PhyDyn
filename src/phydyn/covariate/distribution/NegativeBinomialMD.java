package phydyn.covariate.distribution;

import beast.base.core.Input;
import beast.base.core.Input.Validate;
import org.apache.commons.math3.special.Gamma;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Negative Binomial distribution parameterized by mean (mu) and dispersion (k).
 * This is the common epidemiological parameterization where:
 *   mean = mu, variance = mu + mu^2/k
 * Internally converts to r=k, p=k/(k+mu) for the standard NB formula.
 * Uses continuous relaxation via lgamma for non-integer x.
 */
public class NegativeBinomialMD extends ParamDistribution {

	public final Input<String> meanInput = new Input<>("mean", "Mean of the distribution (mu)", Validate.REQUIRED);
	public final Input<String> dispersionInput = new Input<>("dispersion", "Dispersion parameter (k); smaller k = more overdispersion", Validate.REQUIRED);

	private boolean isMeanConstant, isDispersionConstant;
	private double mean, dispersion;
	private String meanParameter, dispersionParameter;

	private List<String> parameters;

	@Override
	public void initAndValidate() {
		isMeanConstant = isDispersionConstant = true;
		mean = 1; dispersion = 1;
		parameters = new ArrayList<String>();
		String param = meanInput.get().trim();
		try {
			mean = Double.parseDouble(param);
		} catch (Exception e) {
			isMeanConstant = false;
			parameters.add(param);
			meanParameter = param;
		}
		param = dispersionInput.get().trim();
		try {
			dispersion = Double.parseDouble(param);
		} catch (Exception e) {
			isDispersionConstant = false;
			parameters.add(param);
			dispersionParameter = param;
		}
	}

	@Override
	public int numParameters() {
		return parameters.size();
	}

	@Override
	public List<String> getParameters() {
		return new ArrayList<String>(parameters);
	}

	@Override
	public void updateParameters(Map<String, Double> env) {
		if (!isMeanConstant)
			mean = env.get(meanParameter);
		if (!isDispersionConstant)
			dispersion = env.get(dispersionParameter);
	}

	@Override
	public double logDensity(double x) {
		// Convert mean/dispersion to r,p: r=k, p=k/(k+mu)
		double r = dispersion;
		double p = dispersion / (dispersion + mean);
		// log P(x) = lgamma(x+r) - lgamma(r) - lgamma(x+1) + r*log(p) + x*log(1-p)
		return Gamma.logGamma(x + r) - Gamma.logGamma(r) - Gamma.logGamma(x + 1)
				+ r * Math.log(p) + x * Math.log(1 - p);
	}

}
