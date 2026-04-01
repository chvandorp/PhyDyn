package phydyn.covariate;

import beast.base.core.Input;
import beast.base.core.Input.Validate;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import phydyn.covariate.distribution.ParamDistribution;
import phydyn.model.PopModelInterpreter;
import phydyn.model.SemanticChecker;
import phydyn.model.TimeSeriesFGY;
import phydyn.model.parser.PopModelLexer;
import phydyn.model.parser.PopModelParser;
import phydyn.model.parser.PopModelParser.ExprContext;
import phydyn.util.DVector;
import phydyn.util.General;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CovariateLikelihood extends TrajectoryFit {


	public Input<Boolean> quietInput = new Input<>("quiet","Print seropevalences",new Boolean(true));
	public Input<Boolean> ignoreInput = new Input<>("ignore","Ignore SeroP likelihood",new Boolean(false));

	public Input<String> covariateInput = new Input<>(
			 "covariate-expression","Expression denoting the covariate value", Validate.REQUIRED);

	public Input<ParamDistribution>  distInput = new Input<>("covariate-distribution", "Data point distribution");
	public Input<String> distExpInput = new Input<>("distribution-expression", "Data point distribution", Validate.XOR, distInput);

	public Input<String> covDefinitionsInput = new Input<>(
			"covariate-definitions",
			"Semicolon-separated name=expression pairs evaluated at each time point. "
			+ "Expressions can use model state variables and parameters. "
			+ "Example: mu_ALT=rho*A+ALT0;gamma_rate=k/(rho*A+ALT0)");

	boolean ignore, quiet;

	int idxTime, idxCov;
	String idCov, idCovExp;

	// New
	ParamDistribution dist;
	List<String> distVars;


	PopModelInterpreter interpreter;
	ExprContext covExprCtx, distExprCtx;
	boolean useT0T1, useT, useModelParameters;
	double[] t0t1;

	// Covariate definitions: name=expression pairs
	String[] defNames;
	ExprContext[] defExprCtxs;
	int numDefs;

	double[][] data;
	Map<String,Double> localEnv;

	@Override
	public void initAndValidate() {
		super.initAndValidate();

		if (!headerInput.get())
			throw new IllegalArgumentException("header flag must be set to true");

		quiet = quietInput.get();
		ignore = ignoreInput.get();



		idxTime = 0;
		String timeHeader = headers[idxTime];
		// idxTime = General.indexOf(timeHeaderInput.get(), headers);
		if (timeHeader.compareTo("time")!=0 && timeHeader.contains("t")) {
			throw new IllegalArgumentException("CovariateLikelihood: First column must be named 't' or 'time'");
		}
		idxCov = 1;
		idCov = headers[idxCov];
		idCovExp = idCov.concat("Val");


		// Process Covariate Expression
		useT0T1 = useT = useModelParameters = false;
		/* parse equation string */
		CodePointCharStream  input = CharStreams.fromString( covariateInput.get()  );
		try {
			PopModelLexer lexer = new PopModelLexer(input);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			PopModelParser parser = new PopModelParser(tokens);
			covExprCtx =  parser.expr();
		} catch (Exception e) {
			System.out.println( "Error while parsing covariate expression: "+covariateInput.get());
			throw new IllegalArgumentException("Parsing error");
		}
		SemanticChecker checker = new SemanticChecker(popModel);

		// add headers
		for(int i=0; i < headers.length; i++)
			checker.addExternalVariable(headers[i]);

		if (checker.check(covExprCtx)) {
			throw new IllegalArgumentException("Error(s) found in covariate expression");
		}

		// Parse covariate definitions (name=expression pairs)
		numDefs = 0;
		defNames = null;
		defExprCtxs = null;
		if (covDefinitionsInput.get() != null) {
			String defsStr = covDefinitionsInput.get().trim();
			if (defsStr.length() > 0) {
				String[] pairs = defsStr.split(";");
				ArrayList<String> namesList = new ArrayList<>();
				ArrayList<ExprContext> exprsList = new ArrayList<>();
				for (String pair : pairs) {
					pair = pair.trim();
					if (pair.length() == 0) continue;
					String[] parts = pair.split("=", 2);
					if (parts.length != 2)
						throw new IllegalArgumentException(
							"Incorrect syntax in covariate-definitions: " + pair
							+ " (expected name=expression)");
					String name = parts[0].trim();
					String exprStr = parts[1].trim();
					// Parse the expression
					CodePointCharStream defInput = CharStreams.fromString(exprStr);
					ExprContext defExprCtx;
					try {
						PopModelLexer lexer = new PopModelLexer(defInput);
						CommonTokenStream tokens = new CommonTokenStream(lexer);
						PopModelParser parser = new PopModelParser(tokens);
						defExprCtx = parser.expr();
					} catch (Exception e) {
						System.out.println("Error while parsing covariate definition: " + pair);
						throw new IllegalArgumentException("Parsing error in definition: " + pair);
					}
					// Semantic check
					if (checker.check(defExprCtx)) {
						throw new IllegalArgumentException(
							"Error(s) found in covariate definition expression: " + exprStr);
					}
					namesList.add(name);
					exprsList.add(defExprCtx);
					// Register the defined name so it can be used by distributions
					checker.addExternalVariable(name);
				}
				numDefs = namesList.size();
				defNames = namesList.toArray(new String[0]);
				defExprCtxs = exprsList.toArray(new ExprContext[0]);
			}
		}


		// Check that distribution can be computed
		dist = distInput.get();
		if (dist != null) {
			distVars = dist.getParameters();
			for (String id: distVars) {
				// distribution parameters can reference data columns, model state
				// variables, model (MCMC) parameters, or covariate definitions
				boolean found = General.indexOf(id, headers) != -1
						|| General.indexOf(id, popModel.yNames) != -1
						|| General.indexOf(id, popModel.modelParams.paramNames) != -1
						|| (defNames != null && General.indexOf(id, defNames) != -1);
				if (!found)
					throw new IllegalArgumentException("Distribution Parameter not found: "+id
							+ " (checked data columns, state variables, model parameters, and definitions)");
			}
		}
		// This is XOR
		distExprCtx = null;
		if (distExpInput.get() != null) {
			input = CharStreams.fromString(distExpInput.get());
			try {
				PopModelLexer lexer = new PopModelLexer(input);
				CommonTokenStream tokens = new CommonTokenStream(lexer);
				PopModelParser parser = new PopModelParser(tokens);
				distExprCtx =  parser.expr();
			} catch (Exception e) {
				System.out.println( "Error while parsing distribution expression: "+ distExpInput.get());
				throw new IllegalArgumentException("Parsing error");
			}
			checker.addExternalVariable(idCovExp);
			if (checker.check(distExprCtx)) {
				throw new IllegalArgumentException("Error(s) found in distribution expression");
			}
		}



		interpreter = new PopModelInterpreter(checker);

		if (checker.useT0T1) {
			useT0T1 = true;
			t0t1 = new double[2];
		}
		if (checker.useT)
			useT = true;


		// Variables used by expressions
		for (Map.Entry<String, Integer> entry : checker.envTypesUsed.entrySet()) {
		    String id = entry.getKey();
		    int value = entry.getValue();
		    if (value == 3)
		    	useModelParameters = true;
		    if (value == 0) {
		    	throw new IllegalArgumentException("(Not implemented) Illegal use of popmodel "
		    			+ "definition variable in covariate expression: "+id);
		    }
		}

		localEnv = new HashMap<String,Double>();

		data = this.getDataAsMatrix();


	}

	@Override
    public double calculateLogP() {

		TimeSeriesFGY ts = popModel.getTimeSeries();

		if (useT0T1) {
			t0t1[0] = popModel.getStartTime();
			t0t1[1] = popModel.getEndTime();
			interpreter.updateEnv(SemanticChecker.T0T1, t0t1);
		}
		if (useModelParameters) {
			interpreter.updateEnv(popModel.modelParams.paramNames,popModel.modelParams.paramValues );
		}

		int nps = ts.getNumTimePoints();  // reverse time

		int tpStart = nps/2;

		logP = 0;
		DVector Ys = null;
		int tp = tpStart;
		double pointLogP;
		for(int i = 0; i < numrows; i++) {

			final double[] dataRow = data[i];

			final double t = dataRow[this.idxTime];
			final double covData = dataRow[this.idxCov];

			tp = ts.getTimePoint(t, tp);

			if (useT)
				interpreter.updateEnv(SemanticChecker.T, t);

			Ys  = ts.getYall(tp);

			interpreter.updateEnv(popModel.yNames, Ys.data);
			interpreter.updateEnv(headers, dataRow);
			final double covValue = interpreter.evaluate(covExprCtx);

			// Evaluate covariate definitions (compound expressions)
			double[] defValues = null;
			if (numDefs > 0) {
				defValues = new double[numDefs];
				for (int d = 0; d < numDefs; d++) {
					defValues[d] = interpreter.evaluate(defExprCtxs[d]);
					// Update interpreter env so later definitions can reference earlier ones
					interpreter.updateEnv(defNames[d], defValues[d]);
				}
			}

			pointLogP = 0;

			if (dist != null) {
				// data columns
				for(int j = 0; j < headers.length; j++) {
					localEnv.put(headers[j], dataRow[j]);
				}
				// model state variables (T, I, V, A, ...)
				for(int j = 0; j < popModel.yNames.length; j++) {
					localEnv.put(popModel.yNames[j], Ys.data[j]);
				}
				// MCMC model parameters (sigma, k, rho, ...)
				for(int j = 0; j < popModel.modelParams.paramNames.length; j++) {
					localEnv.put(popModel.modelParams.paramNames[j], popModel.modelParams.paramValues[j]);
				}
				// covariate definitions (compound expressions)
				if (numDefs > 0) {
					for (int d = 0; d < numDefs; d++) {
						localEnv.put(defNames[d], defValues[d]);
					}
				}
				dist.updateParameters(localEnv);
				pointLogP = dist.logDensity(covValue);

			} else { // if (distExprCtx !=null)
				interpreter.updateEnv(idCovExp, covValue);
				pointLogP = interpreter.evaluate(distExprCtx);
			}

			logP += pointLogP;

		}

		if (ignore)
			logP=0;

        return logP;
    }




}
