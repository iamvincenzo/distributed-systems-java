package socket.clientserver.assignment2;

import java.io.Serializable;

/**
*
* The class {@code Request} provides a simplified model of a request message.
*
**/
public class Request implements Serializable
{
  private static final long serialVersionUID = 1L;

  private final int value;

  /**
   * Class constructor.
   *
   * @param v  the value.
   *
  **/
  public Request(final int v)
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