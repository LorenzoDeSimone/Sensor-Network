package sensors;

/**
 * Created by civi on 22/04/16.
 */
public class LightSimulator extends Simulator implements Runnable {

    private final double A = 185;
    private final double W = 0.15;
    private final double PHI = rnd.nextDouble();

    public LightSimulator(String id, Buffer<Measurement> measurementsQueue){
        super(id, "light", measurementsQueue);
    }

    @Override
    public void run() {

        double i = 0.1;
        long waitingTime;

        while(!stopCondition){

            double light = getLight(i);
            addMeasurementToQueue(light);

            waitingTime = 1000 + (int)(Math.random()*3000);
            sleep(waitingTime);

            i+=0.2;

        }

    }

    private double getLight(double t){
        return A * Math.sin(W*t+PHI)+210+rnd.nextGaussian()*5;

    }
}