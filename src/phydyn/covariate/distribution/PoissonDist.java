package phydyn.covariate.distribution;

import beast.base.core.Input;
import beast.base.core.Input.Validate;
import org.apache.commons.math3.special.Gamma;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PoissonDist extends ParamDistribution {

	public final Input<String> lambdaInput = new Input<>("lambda", "Poisson distribution rate parameter", Validate.REQUIRED);

	private boolean isLambdaConstant;
	private double lambda;
	private String lambdaParameter;

	private List<String> parameters;

	@Override
	public void initAndValidate() {
		isLambdaConstant = true;
		lambda = 1;
		parameters = new ArrayList<String>();
		String p = lambdaInput.get().trim();
		try {
			lambda = Double.parseDouble(p);
		} catch (Exception e) {
			isLambdaConstant = false;
			parameters.add(p);
			lambdaParameter = p;
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
		if (!isLambdaConstant)
			lambda = env.get(lambdaParameter);
	}

	@Override
	public double logDensity(double x) {
		// log P(x) = x*log(lambda) - lambda - lgamma(x+1)
		return x * Math.log(lambda) - lambda - Gamma.logGamma(x + 1);
	}

}
