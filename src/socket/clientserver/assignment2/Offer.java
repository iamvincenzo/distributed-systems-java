package socket.clientserver.assignment2;

import java.io.Serializable;

/**
*
* The class {@code Offer} provides a simplified model to make/read an offer.
*
**/
public class Offer implements Serializable
{
  private static final long serialVersionUID = 1L;
  private final int value;

  /** 
   * Class constructor.
   *
   * @param v  the value.
   * 
  **/
  public Offer(final int v)
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