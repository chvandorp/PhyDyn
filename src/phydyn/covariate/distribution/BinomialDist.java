package phydyn.covariate.distribution;

import beast.base.core.Input;
import beast.base.core.Input.Validate;
import org.apache.commons.math3.special.Gamma;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Binomial distribution parameterized by n (number of trials) and p (success probability).
 * Uses continuous relaxation via lgamma for non-integer x.
 */
public class BinomialDist extends ParamDistribution {

	public final Input<String> nInput = new Input<>("n", "Number of trials", Validate.REQUIRED);
	public final Input<String> pInput = new Input<>("p", "Success probability", Validate.REQUIRED);

	private boolean isNConstant, isPConstant;
	private double n, p;
	private String nParameter, pParameter;

	private List<String> parameters;

	@Override
	public void initAndValidate() {
		isNConstant = isPConstant = true;
		n = 1; p = 0.5;
		parameters = new ArrayList<String>();
		String param = nInput.get().trim();
		try {
			n = Double.parseDouble(param);
		} catch (Exception e) {
			isNConstant = false;
			parameters.add(param);
			nParameter = param;
		}
		param = pInput.get().trim();
		try {
			p = Double.parseDouble(param);
		} catch (Exception e) {
			isPConstant = false;
			parameters.add(param);
			pParameter = param;
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
		if (!isNConstant)
			n = env.get(nParameter);
		if (!isPConstant)
			p = env.get(pParameter);
	}

	@Override
	public double logDensity(double x) {
		// log P(x) = lgamma(n+1) - lgamma(x+1) - lgamma(n-x+1) + x*log(p) + (n-x)*log(1-p)
		return Gamma.logGamma(n + 1) - Gamma.logGamma(x + 1) - Gamma.logGamma(n - x + 1)
				+ x * Math.log(p) + (n - x) * Math.log(1 - p);
	}

}
