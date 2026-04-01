package phydyn.covariate.distribution;

import beast.base.core.Input;
import beast.base.core.Input.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExponentialDist extends ParamDistribution {

	public final Input<String> rateInput = new Input<>("rate", "Exponential distribution rate parameter (lambda)", Validate.REQUIRED);

	private boolean isRateConstant;
	private double rate;
	private String rateParameter;

	private List<String> parameters;

	@Override
	public void initAndValidate() {
		isRateConstant = true;
		rate = 1;
		parameters = new ArrayList<String>();
		String p = rateInput.get().trim();
		try {
			rate = Double.parseDouble(p);
		} catch (Exception e) {
			isRateConstant = false;
			parameters.add(p);
			rateParameter = p;
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
		if (!isRateConstant)
			rate = env.get(rateParameter);
	}

	@Override
	public double logDensity(double x) {
		// log f(x) = log(lambda) - lambda*x
		return Math.log(rate) - rate * x;
	}

}
