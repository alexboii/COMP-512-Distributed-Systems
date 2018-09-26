package cars.Model;


import Model.ReservableItem;

/**
 * Created by alex on 9/25/18.
 */
public class Car extends ReservableItem
{
    public Car(String location, int count, int price)
    {
        super(location, count, price);
    }

    public String getKey()
    {
        return Car.getKey(getLocation());
    }

    public static String getKey(String location)
    {
        String s = "car-" + location;
        return s.toLowerCase();
    }
}