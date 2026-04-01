package phydyn.covariate.distribution;

import beast.base.core.Input;
import beast.base.core.Input.Validate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GammaDist extends ParamDistribution {

	public final Input<String> shapeInput = new Input<>("shape", "Gamma distribution shape parameter (alpha)", Validate.REQUIRED);
	public final Input<String> rateInput = new Input<>("rate", "Gamma distribution rate parameter (beta)", Validate.REQUIRED);

	private boolean isShapeConstant, isRateConstant;
	private double shape, rate;
	private String shapeParameter, rateParameter;

	private List<String> parameters;

	@Override
	public void initAndValidate() {
		isShapeConstant = isRateConstant = true;
		shape = 1; rate = 1;
		parameters = new ArrayList<String>();
		String p = shapeInput.get().trim();
		try {
			shape = Double.parseDouble(p);
		} catch (Exception e) {
			isShapeConstant = false;
			parameters.add(p);
			shapeParameter = p;
		}
		p = rateInput.get().trim();
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
		if (!isShapeConstant)
			shape = env.get(shapeParameter);
		if (!isRateConstant)
			rate = env.get(rateParameter);
	}

	@Override
	public double logDensity(double x) {
		// log f(x) = alpha*log(beta) - lgamma(alpha) + (alpha-1)*log(x) - beta*x
		return shape * Math.log(rate) - org.apache.commons.math3.special.Gamma.logGamma(shape)
				+ (shape - 1) * Math.log(x) - rate * x;
	}

}
