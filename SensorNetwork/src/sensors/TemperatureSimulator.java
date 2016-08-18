package sensors;

/**
 * Created by civi on 22/04/16.
 */
public class TemperatureSimulator extends Simulator implements Runnable {

    private final double A = 10;
    private final double W = 0.1;
    private final double PHI = rnd.nextDouble();

    public TemperatureSimulator(String id, Buffer<Measurement> measurementsQueue){
        super(id, "temperature", measurementsQueue);
    }

    @Override
    public void run() {

        double i = 0.1;
        long waitingTime;

        while(!stopCondition){

            double temperature = getTemperature(i);
            addMeasurementToQueue(temperature);

            waitingTime = 1000 + (int)(Math.random()*3000);
            sleep(waitingTime);

            i+=0.1;
        }

    }

    private double getTemperature(double t){
        return A * Math.sin(W*t+PHI)+20+rnd.nextGaussian()*0.2;

    }
}
