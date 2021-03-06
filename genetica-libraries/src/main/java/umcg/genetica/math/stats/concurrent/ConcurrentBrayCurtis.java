/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package umcg.genetica.math.stats.concurrent;

import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.DenseLargeDoubleMatrix2D;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import umcg.genetica.console.ProgressBar;
import umcg.genetica.containers.Pair;

/**
 *
 * @author harmjan
 */
public class ConcurrentBrayCurtis {

    private int nrThreads = Runtime.getRuntime().availableProcessors();

    public ConcurrentBrayCurtis() {
    }

    public ConcurrentBrayCurtis(int nrProcs) {
        nrThreads = nrProcs;
    }

    public DoubleMatrix2D pairwiseBrayCurtisDoubleMatrix(double[][] in) {
        ExecutorService threadPool = Executors.newFixedThreadPool(nrThreads);
        CompletionService<Pair<Integer, double[]>> pool = new ExecutorCompletionService<Pair<Integer, double[]>>(threadPool);
        
        for (int i = 0; i < in.length; i++) {
            ConcurrentBrayCurtisTask task = new ConcurrentBrayCurtisTask(in, i);
            pool.submit(task);
        }

        int returned = 0;
        
        DoubleMatrix2D brayCurtisDistanceMatrix;
        if((in.length * (long) in.length) > (Integer.MAX_VALUE - 2)){
            brayCurtisDistanceMatrix = new DenseLargeDoubleMatrix2D(in.length, in.length);
        } else {
            brayCurtisDistanceMatrix = new DenseDoubleMatrix2D(in.length, in.length);
        } 
        
        
        ProgressBar pb = new ProgressBar(in.length, "Calculation of Bray Curtis Distance matrix: " + in.length + " x " + in.length);
        while (returned < in.length) {
            try {
                Pair<Integer, double[]> result = pool.take().get();
                if (result != null) {
                    int rownr = result.getLeft(); //  < 0 when row is not to be included because of hashProbesToInclude.
                    if (rownr >= 0) {
                        double[] doubles = result.getRight();
                        for(int i=0; i<doubles.length; ++i){
                            brayCurtisDistanceMatrix.setQuick(rownr, i, doubles[i]);
                        }
                    }
                    result = null;
                    returned++;
                    pb.iterate();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        for(int r=1;r<brayCurtisDistanceMatrix.rows(); r++){
            for(int c=0; c<r; c++){
                brayCurtisDistanceMatrix.setQuick(r, c, brayCurtisDistanceMatrix.getQuick(c, r));
            }
        }
        
        threadPool.shutdown();
        pb.close();
        return brayCurtisDistanceMatrix;
    }
}
