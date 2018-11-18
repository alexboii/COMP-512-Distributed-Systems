package Model;

import java.io.*;

/**
 * Created by alex on 9/25/18.
 */
// Resource manager data item
public abstract class ResourceItem implements Serializable, Cloneable
{
    ResourceItem()
    {
        super();
    }

    public Object clone()
    {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            return null;
        }
    }
}

