// ModelSupport.java
//
// (c) 2001 PAL Core Development Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)


package net.maizegenetics.pal.statistics;

import net.maizegenetics.pal.io.FormattedOutput;
import net.maizegenetics.pal.math.MersenneTwisterFast;
import net.maizegenetics.pal.report.Report;
import net.maizegenetics.pal.util.HeapSort;

import java.io.PrintWriter;

/**
 * Computes Akaike weights and expected Akaike weights
 * (relative evidence, expected relative evidence)
 * for a set of models and computes corresponding confidence sets 
 *
 * @version $Id: ModelSupport.java,v 1.1 2007/01/12 03:26:16 tcasstevens Exp $
 *
 * @author Korbinian Strimmer
 */
public class ModelSupport implements Report
{
	//
	// Public stuff
	//
		
	/**
	 * likelhood order of the tree hypotheses 
	 */
	public int[] likelihoodOrder;

	/**
	 * support order of the tree hypotheses
	 */
	public int[] supportOrder;

	/**
	 * log-likelihood differences to the best tree
	 */ 
	public double[] deltaL;
	
	/**
	 * posterior probabilities for each hypothesis
	 */ 	
	public double[] posterior;
	
	/**
	 * support in each hypothesis
	 */ 	
	public double[] support;
	
	/**
	 * number of bootstrap replicates
	 */ 	
	public int numBootstraps;

	/** 
	 * Determine posterior probabilties and support values
	 * for each hypothesis and store results in public arrays
	 * posterior, support etc which will automatically be
	 * created by this procedure.
	 *
	 * @param  sLogL       log-likelihoods of each site
	 * @param  numBoot     number of bootstraps
	 */	
	public void compare(double[][] sLogL, int numBoot)
	{
		srtest(sLogL, null, numBoot);
	}

	/** 
	 * Determine posterior probabilties and support values
	 * for each hypothesis and store results in public arrays
	 * posterior, support etc which will automatically be
	 * created by this procedure.
	 *
	 * @param  pLogL       log-likelihoods of each pattern
	 * @param  alias       map of patterns to sites in sequence
	 * @param  numBoot     number of bootstraps
	 */	
	public void compare(double[][] pLogL, int[] alias, int numBoot)
	{
		srtest(pLogL, alias, numBoot);
	}

	public void report(PrintWriter out)
	{
		FormattedOutput fo = FormattedOutput.getInstance();
		
		out.println("CREDIBLE SET " + numBootstraps + " bootstraps):");
		out.println();
		out.println("tree\tdeltaL\tpost\tsupp");
		out.println("--------------------------------");

		for (int i = 0; i < deltaL.length; i++)
		{
			out.print((i+1) + "\t");
			fo.displayDecimal(out, deltaL[i], 2);
			out.print("\t");
			fo.displayDecimal(out, posterior[i], 4);
			out.print("\t");
			fo.displayDecimal(out, support[i], 4);
			out.println();
		}
		out.println();
		out.println("prob = Akaike weight (non-informative posterior probability)");
		out.println("supp = expected Akaike weight");
		
		out.println();
		out.println();
		out.println("supp\toptimal 95% credible sets (expected Akaike weights)");
		out.println("------------------------------------------------------------");
		printSets(out, supportOrder, 0.95);
		
		/*out.println();
		out.println();
		out.println("supp\talternative 95% confidence sets (using deltaL order)");
		out.println("------------------------------------------------------------");
		printSets(out, likelihoodOrder, 0.95);*/
			
		out.println();
	}

	//
	// Private stuff
	//

	private void printSets(PrintWriter out, int[] order, double level)
	{
		FormattedOutput fo = FormattedOutput.getInstance();
		
		double conf = 0.0;
		double conf2;
		int pos = 0;
			
		while (conf < level)
		{
			conf += support[order[pos]];
			
			if (pos+1 < order.length)
			{
				conf2 = conf + support[order[pos+1]];
			}
			else
			{
				conf2 = conf;
			}
			
			if (conf2 >= level)
			{
				fo.displayDecimal(out, conf, 4);
				out.print("\t{" + (order[0]+1));	
				for (int i = 0; i < pos; i++)
				{
					out.print(", " + (order[i+1]+1));
				}
				out.println("}");
			}
				
			pos++;	
		}
	}
		
	private void srtest(double[][] pLogL, int[] alias,  int numBoot)
	{
		// number of hypothesis 
		int numH = pLogL.length;
		
		// allocate public arrays
		deltaL = new double[numH];
		support = new double[numH];
		posterior = new double[numH];
		likelihoodOrder = new int[numH];
		supportOrder = new int[numH];	
		
		// number of bootstrap replicates
		numBootstraps = numBoot;

		// number of sites
		// if alias==null assume one-to-one mapping of sites and patterns
		int numSites;
		if (alias == null)
		{
			numSites = pLogL[0].length;
		}
		else
		{
			numSites = alias.length;
		}
		

		// Compute log-likelihoods, their order, 
		// their deltas and their posteriors
		for (int j = 0; j < numSites; j++)
		{
			int p;
			if (alias == null)
			{
				p = j;
			}
			else
			{
				p = alias[j];
			}
				
			for (int k = 0; k < numH; k++)
			{
				deltaL[k] -= pLogL[k][p];
			}
		}
		HeapSort.sort(deltaL, likelihoodOrder);

		// Compute deltas
		double maxL= -deltaL[likelihoodOrder[0]];
		for (int j = 0; j < numH; j++)
		{
			deltaL[j] = -(deltaL[j]+maxL);
		}
		
		// compute posterior probabilities 
		double sum1 = 0;
		for (int j = 0; j < numH; j++)
		{
			posterior[j] = Math.exp(deltaL[j]);
			sum1 += posterior[j];
		}
		for (int j = 0; j < numH; j++)
		{
			posterior[j] = posterior[j]/sum1;
		}
		
		// reverse sign of delta L
		for (int j = 0; j < numH; j++)
		{
			deltaL[j] = -deltaL[j];
		}
		deltaL[likelihoodOrder[0]] = 0.0;
		
	
		// Resample data

		// temporary memory 
		double[] rs = new double[numH];
		
		MersenneTwisterFast mt = new MersenneTwisterFast();
		for (int i = 0; i < numBoot; i++)
		{
			for (int k = 0; k < numH; k++)
			{
					rs[k] = 0;
			}

			for (int j = 0; j < numSites; j++)
			{
				int s = mt.nextInt(numSites);
				
				int p;
				if (alias == null)
				{
					p = s;
				}
				else
				{
					p = alias[s];
				}
				
				for (int k = 0; k < numH; k++)
				{
					rs[k] += pLogL[k][p];
				}
			}
			
			// find ml hypothesis
			double maxLogL = findMax(rs);
			
			// compute log-likelihood difference
			for (int k = 0; k < numH; k++)
			{
				rs[k] = rs[k] - maxLogL;
			}

			// compute posteriors and sum over resampleds data set
			double sum = 0;
			for (int k = 0; k < numH; k++)
			{
				rs[k] = Math.exp(rs[k]);
				sum += rs[k];
			}
			for (int k = 0; k < numH; k++)
			{
				support[k] += rs[k]/sum;
			}
		}  
		
		// compute support values
		for (int j = 0; j < numH; j++)
		{
			support[j] = support[j]/numBoot;
		}

		// determine order of support (smallest->largest)
		HeapSort.sort(support, supportOrder);
		
		// reversing the order 
		int len = supportOrder.length;
		for (int i = 0; i < len/2; i++)
		{
			int tmp = supportOrder[i];
			supportOrder[i] = supportOrder[len-i-1];
			supportOrder[len-i-1] = tmp;
		}
		
		// free temporary memory
		rs = null;
	}
	
	private double findMax(double[] array)
	{
		int len = array.length;
		
		int best = 0;
		double max = array[0];
		for (int i = 1; i < len; i++)
		{
			if (array[i] > max)
			{
				best = i;
				max = array[i];
			}
		}
		
		return max;
	}
}
