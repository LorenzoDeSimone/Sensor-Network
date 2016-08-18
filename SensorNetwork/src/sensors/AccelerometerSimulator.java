package sensors;

/**
 * Created by civi on 22/04/16.
 */
public class AccelerometerSimulator extends Simulator {

    private final double A1 = 5;
    private final double W1 = 10;
    private final double PHI1 = rnd.nextDouble();
    private final double A2 = 20;
    private final double W2 = 5;
    private final double PHI2 = rnd.nextDouble();
    private final double MOTIONPROB = 0.05;

    public AccelerometerSimulator(String id, Buffer<Measurement> measurementsQueue){
        super(id, "accelerometer", measurementsQueue);
    }

    @Override
    public void run() {

        long waitingTime = 200;

        double i = 0.1, j=0.1;
        boolean inMotion = false;
        double randomAcceleration = 0.;


        while(!stopCondition){

            inMotion = Math.random()<MOTIONPROB ? !inMotion: inMotion;

            if(!inMotion)
                randomAcceleration = (int)(Math.random()*500);

            double accelerometer = getAcceleration(i, j, randomAcceleration, inMotion);
            addMeasurementToQueue(accelerometer);

            sleep(waitingTime);

            i+=0.01;
            j+=0.8;

        }
    }

    private double getAcceleration(double t1, double t2, double randomAcceleration, boolean inMotion){

        double motionAcceleration = 0.;

        if(inMotion)
            motionAcceleration = A2*Math.sin(W2*t2+PHI2)+randomAcceleration+rnd.nextGaussian()*3;

        return A1 * Math.sin(W1*t1+PHI1)+50+rnd.nextGaussian()*0.2 +motionAcceleration;
    }
}
