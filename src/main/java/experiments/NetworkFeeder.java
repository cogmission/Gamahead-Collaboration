package experiments;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.network.Inference;
import org.numenta.nupic.network.Network;

import rx.Subscriber;


public class NetworkFeeder {
    private Network network;
    
    /**
     * Creates and returns a {@link Network} for demo processing
     * @return
     */
    public Network createNetwork() {
        org.numenta.nupic.Parameters temporalParams = createParameters();
        network = Network.create("Cortical.io API Demo", temporalParams)
            .add(Network.createRegion("Region 1")
                .add(Network.createLayer("Layer 2/3", temporalParams)
                    .add(new TemporalMemory())
                    .add(new SpatialPooler())));
        return network;            
    }
    
    /**
     * Creates and returns a {@link Parameters} object configured
     * for this demo.
     * 
     * @return
     */
    private Parameters createParameters() {
        Parameters p = org.numenta.nupic.Parameters.getAllDefaultParameters();
        
        p.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        p.setParameterByKey(KEY.CELLS_PER_COLUMN, 32);
        p.setParameterByKey(KEY.INPUT_DIMENSIONS, new int[] { 884 });
        p.setParameterByKey(KEY.GLOBAL_INHIBITIONS, true);
        p.setParameterByKey(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 40.0);
        p.setParameterByKey(KEY.POTENTIAL_PCT, 0.8);
        p.setParameterByKey(KEY.SYN_PERM_CONNECTED,0.1);
        p.setParameterByKey(KEY.SYN_PERM_ACTIVE_INC, 0.0001);
        p.setParameterByKey(KEY.SYN_PERM_INACTIVE_DEC, 0.0005);
        p.setParameterByKey(KEY.MAX_BOOST, 1.0);
        
        p.setParameterByKey(KEY.CONNECTED_PERMANENCE, 0.5);
        p.setParameterByKey(KEY.INITIAL_PERMANENCE, 0.4);
        p.setParameterByKey(KEY.MIN_THRESHOLD, 164);
        p.setParameterByKey(KEY.MAX_NEW_SYNAPSE_COUNT, 164);
        p.setParameterByKey(KEY.PERMANENCE_INCREMENT, 0.1);
        p.setParameterByKey(KEY.PERMANENCE_DECREMENT, 0.0);
        p.setParameterByKey(KEY.ACTIVATION_THRESHOLD, 164);
        
        return p;
    }

    private int[][] getArrayFromFile(String path) {
        int[][] retVal = null;
        
        try {
            Stream<String> s = Files.lines(Paths.get(getClass().getResource(path).getPath()));
            
            @SuppressWarnings("resource")
            int[][] ia = s.map(l -> l.split("[\\s]*\\,[\\s]*")).map(i -> {
                return Arrays.stream(i).mapToInt(Integer::parseInt).toArray();
            }).toArray(int[][]::new);  
            
            retVal = ia;
            
            s.close();
        }catch(Exception e) {
            e.printStackTrace();
        }
        return retVal;
    }
    
    public static void main(String[] args) {
        NetworkFeeder feeder = new NetworkFeeder();
        Network network = feeder.createNetwork();
        
        network.observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() { System.out.println("On completed reached!"); }
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) { 
                System.out.println("sdr = " + Arrays.toString((int[])i.getSDR()));
                System.out.println("prediction = " + Arrays.toString((int[])i.getPredictedColumns()));
                System.out.println("sparseActives = " + Arrays.toString((int[])i.getSparseActives()));
                System.out.println("input = " + Arrays.toString((int[])i.getLayerInput())); 
            }
        });
        
        int[][] input = feeder.getArrayFromFile("1_100.csv");
  
        for(int i = 0;i < 2;i++) {
            for(int[] sdr : input) {
                network.compute(sdr);
            }
            network.reset();
        }
    }
}
