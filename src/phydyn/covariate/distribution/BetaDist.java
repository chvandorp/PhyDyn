package phydyn.covariate.distribution;

import beast.base.core.Input;
import beast.base.core.Input.Validate;
import org.apache.commons.math3.special.Gamma;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Beta distribution parameterized by alpha and beta.
 * log f(x) = lgamma(alpha+beta) - lgamma(alpha) - lgamma(beta) + (alpha-1)*log(x) + (beta-1)*log(1-x)
 */
public class BetaDist extends ParamDistribution {

	public final Input<String> alphaInput = new Input<>("alpha", "Beta distribution alpha parameter", Validate.REQUIRED);
	public final Input<String> betaInput = new Input<>("beta", "Beta distribution beta parameter", Validate.REQUIRED);

	private boolean isAlphaConstant, isBetaConstant;
	private double alpha, beta;
	private String alphaParameter, betaParameter;

	private List<String> parameters;

	@Override
	public void initAndValidate() {
		isAlphaConstant = isBetaConstant = true;
		alpha = 1; beta = 1;
		parameters = new ArrayList<String>();
		String p = alphaInput.get().trim();
		try {
			alpha = Double.parseDouble(p);
		} catch (Exception e) {
			isAlphaConstant = false;
			parameters.add(p);
			alphaParameter = p;
		}
		p = betaInput.get().trim();
		try {
			beta = Double.parseDouble(p);
		} catch (Exception e) {
			isBetaConstant = false;
			parameters.add(p);
			betaParameter = p;
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
		if (!isAlphaConstant)
			alpha = env.get(alphaParameter);
		if (!isBetaConstant)
			beta = env.get(betaParameter);
	}

	@Override
	public double logDensity(double x) {
		return Gamma.logGamma(alpha + beta) - Gamma.logGamma(alpha) - Gamma.logGamma(beta)
				+ (alpha - 1) * Math.log(x) + (beta - 1) * Math.log(1 - x);
	}

}
