package socket.clientserver.assignment2;

import java.io.Serializable;

/**
*
* The class {@code Price} provides a simplified model to make/read a price.
*
**/
public class Price implements Serializable
{
  private static final long serialVersionUID = 1L;
  private final int value;

  /**
   * Class constructor.
   *
   * @param v  the value.
   *
  **/
  public Price(final int v)
  {
    this.value = v;
  }

  /**
   * Gets the value.
   *
   * @return the value.
   *
  **/
  public int getValue()
  {
    return this.value;
  }
}