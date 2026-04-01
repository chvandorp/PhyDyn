package phydyn.covariate.distribution;

import beast.base.core.Input;
import beast.base.core.Input.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bernoulli distribution parameterized by p (success probability).
 * Uses continuous relaxation: log f(x) = x*log(p) + (1-x)*log(1-p)
 */
public class Bernoulli extends ParamDistribution {

	public final Input<String> pInput = new Input<>("p", "Success probability", Validate.REQUIRED);

	private boolean isPConstant;
	private double p;
	private String pParameter;

	private List<String> parameters;

	@Override
	public void initAndValidate() {
		isPConstant = true;
		p = 0.5;
		parameters = new ArrayList<String>();
		String param = pInput.get().trim();
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
		if (!isPConstant)
			p = env.get(pParameter);
	}

	@Override
	public double logDensity(double x) {
		// log P(x) = x*log(p) + (1-x)*log(1-p)
		return x * Math.log(p) + (1 - x) * Math.log(1 - p);
	}

}
