package sensors;

import java.util.Calendar;
import java.util.Random;

/**
 * Created by civi on 22/04/16.
 */
public abstract class Simulator implements Runnable {

    protected volatile boolean stopCondition = false;
    protected Random rnd = new Random();
    private long midnight;
    private Buffer<Measurement> measurementsQueue;
    private String id;
    private String type;

    public Simulator(String id, String type, Buffer<Measurement> measurementsQueue){
        this.id = id;
        this.type = type;
        this.measurementsQueue = measurementsQueue;
        this.midnight = computeMidnightMilliseconds();
    }

    public void stopMeGently() {
        stopCondition = true;
    }

    protected void addMeasurementToQueue(double measurement){
        measurementsQueue.add(new Measurement(id, type, measurement + "", deltaTime()));
    }

    protected void sleep(long milliseconds){
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private long computeMidnightMilliseconds(){
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long deltaTime(){
        return System.currentTimeMillis()-midnight;
    }

}

