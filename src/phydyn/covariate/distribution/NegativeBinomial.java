package phydyn.covariate.distribution;

import beast.base.core.Input;
import beast.base.core.Input.Validate;
import org.apache.commons.math3.special.Gamma;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Negative Binomial distribution parameterized by number of successes r and
 * success probability p.
 * PMF: P(x|r,p) = Gamma(x+r)/(Gamma(r)*Gamma(x+1)) * p^r * (1-p)^x
 * Uses continuous relaxation via lgamma for non-integer x.
 */
public class NegativeBinomial extends ParamDistribution {

	public final Input<String> rInput = new Input<>("r", "Number of successes (dispersion parameter)", Validate.REQUIRED);
	public final Input<String> pInput = new Input<>("p", "Success probability", Validate.REQUIRED);

	private boolean isRConstant, isPConstant;
	private double r, p;
	private String rParameter, pParameter;

	private List<String> parameters;

	@Override
	public void initAndValidate() {
		isRConstant = isPConstant = true;
		r = 1; p = 0.5;
		parameters = new ArrayList<String>();
		String param = rInput.get().trim();
		try {
			r = Double.parseDouble(param);
		} catch (Exception e) {
			isRConstant = false;
			parameters.add(param);
			rParameter = param;
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
		if (!isRConstant)
			r = env.get(rParameter);
		if (!isPConstant)
			p = env.get(pParameter);
	}

	@Override
	public double logDensity(double x) {
		// log P(x) = lgamma(x+r) - lgamma(r) - lgamma(x+1) + r*log(p) + x*log(1-p)
		return Gamma.logGamma(x + r) - Gamma.logGamma(r) - Gamma.logGamma(x + 1)
				+ r * Math.log(p) + x * Math.log(1 - p);
	}

}
