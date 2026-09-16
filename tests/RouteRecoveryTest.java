import dev.cobbledeep.pathfinding.RouteRecovery;
import dev.cobbledeep.pathfinding.SweptBody;
import dev.cobbledeep.pathfinding.SweptBody.Box;
import java.util.Random;

public final class RouteRecoveryTest
{
    private static int checks;
    private static void check(boolean ok, String message)
    { checks++; if (!ok) throw new AssertionError(message); }

    private static boolean overlap(Box a, double x, double y, double z, Box b)
    {
        return a.minX()+x < b.maxX() && a.maxX()+x > b.minX()
                && a.minY()+y < b.maxY() && a.maxY()+y > b.minY()
                && a.minZ()+z < b.maxZ() && a.maxZ()+z > b.minZ();
    }

    public static void main(String[] args)
    {
        Box body = new Box(0,0,0,.6,1.8,.6);
        Box beside = new Box(0,0,1.3,.1,1.8,1.6);
        check(overlap(new Box(0,0,0,1.6,1.8,1.6),0,0,0,beside), "Old enclosing rectangle rejects this clear route");
        check(!SweptBody.hits(body,1,0,1,beside), "Actual diagonal footprint misses side obstruction");
        check(SweptBody.hits(body,1,0,1,new Box(.9,0,.9,1.1,2,1.1)), "Obstacle directly on diagonal remains blocked");
        check(SweptBody.hits(body,1,0,0,new Box(.9,0,0,.91,2,.6)), "Thin door cannot be skipped between samples");
        check(!SweptBody.hits(body,1,0,0,new Box(0,-1,0,2,0,2)), "Ground contact does not block walking");
        check(!SweptBody.hits(body,-1,0,0,new Box(.6,0,0,1,2,.6)), "Moving away from touching wall is allowed");
        check(SweptBody.hits(body,1,0,0,new Box(.6,0,0,1,2,.6)), "Moving into touching wall remains blocked");
        check(SweptBody.hits(body,0,0,0,new Box(.2,.2,.2,.3,.3,.3)), "Initial overlap is blocked");
        check(!SweptBody.hits(body,0,0,0,beside), "Stationary clear body stays clear");
        check(SweptBody.hits(body,0,1,0,new Box(0,2,0,1,2.1,1)), "Ceiling blocks ascent");
        Random random = new Random(3847);
        for (int trial=0; trial<300; trial++)
        {
            double dx=random.nextDouble()*3-1.5, dz=random.nextDouble()*3-1.5;
            double x=random.nextDouble()*4-2, z=random.nextDouble()*4-2;
            Box obstacle=new Box(x,0,z,x+.1+random.nextDouble(),2,z+.1+random.nextDouble());
            boolean swept=SweptBody.hits(body,dx,0,dz,obstacle);
            boolean sampled=false;
            for (int sample=0; sample<=1000; sample++)
                if (overlap(body,dx*sample/1000.0,0,dz*sample/1000.0,obstacle)) { sampled=true; break; }
            check(!sampled || swept, "Continuous sweep never misses a sampled collision");
        }
        RouteRecovery recovery = new RouteRecovery();
        check(recovery.retry(0,0,0), "First local retry allowed");
        recovery.reachedWaypoint(.1,0,0);
        check(recovery.retry(.1,0,0), "Second local retry allowed");
        recovery.reachedWaypoint(.1,0,0);
        check(!recovery.retry(.1,0,0), "Repeated same-place failure remains bounded");
        recovery.reachedWaypoint(1.1,0,0);
        check(recovery.retry(1.1,0,0), "Real progress restores recovery allowance");
        for (int step=2; step<20; step++)
        {
            recovery.reachedWaypoint(step+.1,0,0);
            check(recovery.retry(step+.1,0,0), "Separate obstacles do not exhaust whole-trip budget");
        }
        recovery.clear();
        check(recovery.retry(0,0,0), "First retry before direct travel allowed");
        check(recovery.retry(.1,0,0), "Nearby retry still uses local allowance");
        check(recovery.retry(1,0,0), "Retry itself recognizes travel to a new area");
        recovery.clear();
        check(recovery.retry(0,0,0), "New destination resets retries");
        System.out.println("Passed " + checks + " swept-collision and recovery checks.");
    }
}
