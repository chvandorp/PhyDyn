package phydyn.covariate.distribution;

import beast.base.core.Input;
import beast.base.core.Input.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Log-Normal distribution parameterized by mu and sigma (parameters of the log-scale normal).
 * log f(x) = -log(x) - log(sigma) - 0.5*log(2*pi) - (log(x)-mu)^2 / (2*sigma^2)
 */
public class LogNormalDist extends ParamDistribution {

	public final Input<String> muInput = new Input<>("mu", "Log-Normal location parameter (mean of log)", Validate.REQUIRED);
	public final Input<String> sigmaInput = new Input<>("sigma", "Log-Normal scale parameter (std dev of log)", Validate.REQUIRED);

	private boolean isMuConstant, isSigmaConstant;
	private double mu, sigma;
	private String muParameter, sigmaParameter;

	private List<String> parameters;

	@Override
	public void initAndValidate() {
		isMuConstant = isSigmaConstant = true;
		mu = 0; sigma = 1;
		parameters = new ArrayList<String>();
		String p = muInput.get().trim();
		try {
			mu = Double.parseDouble(p);
		} catch (Exception e) {
			isMuConstant = false;
			parameters.add(p);
			muParameter = p;
		}
		p = sigmaInput.get().trim();
		try {
			sigma = Double.parseDouble(p);
		} catch (Exception e) {
			isSigmaConstant = false;
			parameters.add(p);
			sigmaParameter = p;
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
		if (!isMuConstant)
			mu = env.get(muParameter);
		if (!isSigmaConstant)
			sigma = env.get(sigmaParameter);
	}

	@Override
	public double logDensity(double x) {
		double logx = Math.log(x);
		double diff = logx - mu;
		return -logx - Math.log(sigma * Math.sqrt(2 * Math.PI)) - 0.5 * diff * diff / (sigma * sigma);
	}

}
