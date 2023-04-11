package fit4s.activities.h2;

public class Functions {
    public static double C = ((double) Integer.MAX_VALUE) + 1;

    public static double scToDeg(double sc) {
        return sc * 180d / C;
    }

    public static double scToRad(double sc) {
        return sc * Math.PI / C;
    }

    public static double hav(double lat1, double lng1, double lat2, double lng2) {
        double lat1r = scToRad(lat1);
        double lat2r = scToRad(lat2);

        double dlat = scToRad(lat2 - lat1);
        double dlng = scToRad(lng2 - lng1);

        double a = (Math.sin(dlat / 2) * Math.sin(dlat / 2)) + Math.cos(lat1r) * Math.cos(lat2r) * (Math.sin(dlng / 2) * Math.sin(dlng / 2));
        double angle = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return 6371 * angle;
    }
}
