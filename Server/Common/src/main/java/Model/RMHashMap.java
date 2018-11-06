package Model;

import java.util.*;

// A specialization of HashMap with some extra diagnostics
public class RMHashMap extends HashMap<String, ResourceItem>
{
    public RMHashMap() {
        super();
    }

    public String toString()
    {
        String s = "--- BEGIN RMHashMap ---\n";
        for (String key : keySet())
        {
            String value = get(key).toString();
            s = s + "[KEY='" + key + "']" + value + "\n";
        }
        s = s + "--- END RMHashMap ---";
        return s;
    }

    public void dump()
    {
        System.out.println(toString());
    }

    public Object clone()
    {
        RMHashMap obj = new RMHashMap();
        for (String key : keySet())
        {
            obj.put(key, (ResourceItem)get(key).clone());
        }
        return obj;
    }
}